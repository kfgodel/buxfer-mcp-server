# Buxfer MCP Server — Kotlin Implementation

A Kotlin/JVM implementation of the Buxfer MCP server. Uses the official
[MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) and
[Ktor](https://ktor.io/) for HTTP calls to the Buxfer REST API.

## Conventions

Follow these when adding new endpoints, models, or tests.

### Three-tier nullability

Schemas in `kotlin/src/main/kotlin/com/buxfer/mcp/api/models/` exist for **drift detection**, not as data carriers. Each field's declaration encodes a different drift signal:

| Declaration                  | Key required in JSON? | Value can be `null`? | Use for                                                        |
|------------------------------|-----------------------|----------------------|----------------------------------------------------------------|
| `field: Type`                | yes                   | no                   | Fixture-always-present with non-null value (the common case).  |
| `field: Type?` *(no default)*| **yes**               | yes                  | Key always present in fixture, value sometimes `null`.         |
| `field: Type? = null`        | no                    | yes                  | Spec-only / forward-compat — key may be absent entirely.       |

Rationale: a missing default makes the field required by deserialization (absence triggers `MissingFieldException`). The type's `?` is independent — it controls whether `null` is a valid *value*. Combining the two encodes "the API always sends this key, sometimes with a null value" as a distinct signal from "the API may or may not send this key at all".

### Fixture rules + spec-forward-compat

When deciding nullability:

- **Required** (Tier 1) only when the captured fixture has the field on every record with a non-null value. Fixture is ground truth.
- **Required key, nullable value** (Tier 2) when the fixture always has the key but its value is sometimes `null`.
- **Optional / spec-only** (Tier 3, `Type? = null`) for fields the Buxfer API spec mentions but no captured fixture carries. Keeps the schema forward-compatible (the validator accepts the new field gracefully) and serves as documentation of what the spec promises.

If the API doc and fixture disagree, the fixture wins for required-fields decisions; spec-only divergent fields go in as Tier 3.

### Model-as-schema validation

`BuxferClient` returns raw `JsonElement` (`JsonArray` for list endpoints, `JsonObject` for the wrapper-shaped `/transactions` and the write endpoints). The data path forwards the raw response to Claude unchanged.

Validation runs as a **side effect** via the `validateSchema<T>` helper in `BuxferClient`:

- Strict decode against the schema using a dedicated `validatorJson` (`ignoreUnknownKeys = false`) — strict so unknown fields surface as drift.
- `SerializationException` → `log.warn("Schema drift on ...")`. Never thrown.
- Any other exception → `log.error("Schema validation failed unexpectedly ...")`. Also never thrown.
- The data path uses the production `buxferJson` (permissive `ignoreUnknownKeys = true`) and is never affected by validation outcomes.

The shared `getValidatedList<Schema>(path)` helper handles the common shape (HTTP GET → envelope strip → array extract → validate → return). Endpoints with non-standard shapes (currently `getTransactions` with its `transactions[]` + `numTransactions` wrapper, and the write endpoints with their `JsonObject` responses) inline the same pattern: HTTP, envelope strip, `validateSchema<Schema>(body, path)`, return body.

### Other invariants worth preserving

- **No stdout output anywhere.** MCP communicates over stdio; any `print`/`println`/`System.out` breaks the protocol. Logback config is file-only by default; fatal startup errors go to stderr.
- **`@Volatile` for the auth token**, set once on login and read many times. No other shared mutable state in `BuxferClient`.
- **`AutoCloseable` resources use `.use { ... }`** in tests; the long-running server closes via JVM shutdown hook in `Main.kt`.
- **No star imports.** No multi-class files.
- **Run gradle via ASDF shims**: `PATH="$HOME/.asdf/shims:$PATH" gradle test` from `kotlin/`.

## MCP Kotlin SDK Reference

When working on server bootstrap or tool registration, consult these references before guessing API shapes:

- **Source repo:** https://github.com/modelcontextprotocol/kotlin-sdk
- **Server source (current main):** https://github.com/modelcontextprotocol/kotlin-sdk/blob/main/kotlin-sdk-server/src/main/kotlin/io/modelcontextprotocol/kotlin/sdk/server/Server.kt
- **MCP protocol spec:** https://modelcontextprotocol.io/

### Key types and their packages (SDK 0.11.x)

| Symbol                                                                  | Package                                     |
|-------------------------------------------------------------------------|---------------------------------------------|
| `Server`, `ServerOptions`, `StdioServerTransport`                       | `io.modelcontextprotocol.kotlin.sdk.server` |
| `Implementation`, `ServerCapabilities`, `CallToolResult`, `TextContent` | `io.modelcontextprotocol.kotlin.sdk.types`  |

### Server bootstrap pattern

```kotlin
val server = Server(
    serverInfo = Implementation(name = "...", version = "..."),
    options = ServerOptions(capabilities = ServerCapabilities())
)

server.addTool(
    name = "tool_name",
    description = "..."
) { request ->
    // request.arguments is a JsonObject? of named tool inputs
    CallToolResult(content = listOf(TextContent(text = "...")))
}

// Start the session over stdio
val transport = StdioServerTransport(
    inputStream = System.`in`.asSource().buffered(),
    outputStream = System.out.asSink().buffered()
)
server.createSession(transport)  // suspend; returns ServerSession
```

`StdioServerTransport` requires kotlinx-io `Source`/`Sink` — convert from `java.io.InputStream`/`OutputStream` with `asSource()`/`asSink()` from `kotlinx.io`.

### SDK gotchas (things `hover` won't tell you)

- **`ServerCapabilities()` no-args defaults `tools = null`**, which makes `Server.addTool(...)` throw `IllegalStateException: Server does not support tools capability`. Pass `ServerCapabilities(tools = ServerCapabilities.Tools())` to actually enable tool registration.
- **No `InMemoryTransport` ships with the SDK** (verified in 0.11.1). A test that wants in-process server↔client wiring has to roll its own by extending `AbstractTransport` (override `start`/`send`/`close`; dispatch inbound messages by invoking `_onMessage`).
- **`Server.createSession` returns immediately**; the transport's read/write coroutines run on its own internal `SupervisorJob` scope. `BuxferMcpServer.start` therefore suspends on a `CompletableDeferred<Unit>` completed by `session.onClose`, otherwise `Main`'s `runBlocking` would unwind before any client frame arrives and the JVM would exit with the MCP client reporting "Failed to connect".
- **`KotlinLogging.<clinit>` writes a banner to `System.out`** the first time any code references it (the MCP SDK uses `KotlinLogging.logger { }` via top-level `val`s). That banner would corrupt MCP's first JSON-RPC frame. `Main.kt` flips `KotlinLoggingConfiguration.logStartupMessage = false` as its first executable statement to suppress it; `kotlin-logging-jvm` is therefore a direct dependency (it's already pulled in transitively by the MCP SDK; the direct entry just gives us compile-time access to the flag).

## Prerequisites

- Java 21 and Gradle 9.4.1, both managed via [ASDF](https://asdf-vm.com/).

```bash
# From the kotlin/ directory
asdf plugin add java    # if not already added
asdf plugin add gradle  # if not already added
asdf install            # installs versions declared in .tool-versions
```

- [kotlin-language-server](https://github.com/fwcd/kotlin-language-server) for LSP support in Claude Code:

Only if not present:
```bash
# 1. Install the language server binary
brew install JetBrains/utils/kotlin-lsp

# 2. Install and enable the Claude Code plugin (run once per machine)
claude plugin install kotlin-lsp@claude-plugins-official
claude plugin enable kotlin-lsp
```

The LSP project configuration is already committed at `.claude/settings.json`.

To verify it's working, check `~/.claude/debug/` for a line like:
```
LSP server plugin:kotlin-lsp:kotlin initialized
```
Or simply ask Claude Code.

## Code Intelligence — LSP First

**Always use the LSP tool as the primary way to explore code.** Before guessing an import path, checking a type, or navigating to a definition, use LSP:

- `workspaceSymbol` — find any class, function, or property by name across the whole project and indexed SDK deps
- `goToDefinition` — jump to where a symbol is defined (works for SDK classes once the project compiles)
- `hover` — inspect type signatures and documentation in place
- `findReferences` / `documentSymbol` — explore usages or the structure of a file

**When LSP cannot resolve a symbol** (usually because the project has not compiled yet and external dependencies are not indexed), **ask the user** rather than resorting to `find`, `grep` on `.class` files, or other workarounds. The user can quickly look up the correct import from IDE tooling or documentation.

### SDK API discovery

`build.gradle.kts` applies the `idea` plugin with `isDownloadSources = true`, so Gradle resolves the `*-sources.jar` for every SDK dependency and the LSP indexes them. **Use `hover` and `goToDefinition` first.** They reach the actual SDK source — no jar extraction needed.

If LSP can't resolve a symbol (usually because the project hasn't compiled yet), the source jars live in the Gradle cache, e.g.:

```
~/.gradle/caches/modules-2/files-2.1/io.modelcontextprotocol/kotlin-sdk-server-jvm/<version>/<hash>/kotlin-sdk-server-jvm-<version>-sources.jar
```

Extract to a scratch dir and `Read` files normally. This should be rare.

## Running Gradle

Always use the ASDF shims so the correct Java and Gradle versions from `.tool-versions` are used:

```bash
# From kotlin/
PATH="$HOME/.asdf/shims:$PATH" gradle test
PATH="$HOME/.asdf/shims:$PATH" gradle build
PATH="$HOME/.asdf/shims:$PATH" gradle compileKotlin
```

## Project Structure

```
kotlin/
├── .tool-versions                      # ASDF: java + gradle versions
├── build.gradle.kts                    # Build config, dependencies
├── settings.gradle.kts                 # Project name
└── src/main/kotlin/com/buxfer/mcp/
    ├── Main.kt                         # Entry point — Env.load → login → start MCP server
    ├── Env.kt                          # Loads `.env` (or `--env-file=<path>`) into BuxferMcpConfig
    ├── BuxferMcpConfig.kt              # Typed env-derived config (credentials, optional knobs)
    ├── BuxferMcpServer.kt              # Server bootstrap, registers the 12 MCP tools
    ├── api/
    │   ├── BuxferClient.kt             # Ktor HTTP client, token management, response unwrap
    │   ├── BuxferClientConfig.kt       # HTTP client settings (timeouts, base URL, engine)
    │   ├── BuxferApiException.kt       # Wrapper for Buxfer API + transport failures
    │   └── models/                     # @Serializable drift-detection schemas (one class per file)
    └── tools/
        ├── *Tools.kt                   # One class per Buxfer resource (Account, Tag, Transaction, ...)
        └── JsonObjectExtensions.kt     # require* / opt* helpers for tool args
```

## Key Dependencies

See `build.gradle.kts` for pinned versions.

| Library                                            | Purpose                                                                    |
|----------------------------------------------------|----------------------------------------------------------------------------|
| `io.modelcontextprotocol:kotlin-sdk` (0.11.1)      | MCP server protocol (stdio transport)                                      |
| `io.ktor:ktor-client-cio`                          | Async HTTP client for Buxfer API calls                                     |
| `io.ktor:ktor-client-content-negotiation`          | JSON content negotiation                                                   |
| `io.ktor:ktor-serialization-kotlinx-json`          | JSON serialization via Ktor                                                |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | Data class serialization                                                   |
| `ch.qos.logback:logback-classic`                   | SLF4J binding; rolling-file logging — see `src/main/resources/logback.xml`. Never logs to stdout (the MCP transport); writes to `${BUXFER_LOG_DIR:-./logs}/server.log`. |
| `io.github.oshai:kotlin-logging-jvm`               | Pulled in transitively by the MCP SDK; declared as a direct dependency only so `Main.kt` can flip `KotlinLoggingConfiguration.logStartupMessage` before any SDK class triggers `KotlinLogging.<clinit>` (see SDK gotchas above). |

## Configuration

Configuration is loaded from a `.env` file at startup by `Env.load`. By default
the loader reads `./.env` (relative to the JVM's working directory); pass
`--env-file=<path>` on the JVM command line to point at a different file. The
loader populates a typed `BuxferMcpConfig` for the credentials and optional
overrides, and also pushes `BUXFER_LOG_DIR` / `BUXFER_LOG_LEVEL` into JVM
system properties so Logback's `${VAR}` substitution in `logback.xml` resolves
correctly. Real OS env vars and `-D` system properties always win over the
file (standard precedence).

| Variable           | Required | Default                       | Description                                                                                                                                                                            |
|--------------------|----------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BUXFER_EMAIL`     | yes      | —                             | Buxfer account email. **Never logged** (PII / account identifier).                                                                                                                     |
| `BUXFER_PASSWORD`  | yes      | —                             | Buxfer account password. **Never logged**.                                                                                                                                             |
| `BUXFER_LOG_DIR`   | no       | `./logs`                      | Directory the rolling log appender writes to. **Resolved relative to the JVM's cwd at launch**, which is whatever directory the launching tool (Claude Code, Claude Desktop, a shell) happened to start from — not necessarily `kotlin/`. Set to an absolute path in `.env` if you want a stable location. |
| `BUXFER_LOG_LEVEL` | no       | `INFO`                        | Logback root level. Lowering to `DEBUG` enables HTTP request/response logging in `BuxferClient` and per-arg value logging in tools (the always-redacted set — password, token, statement — stays redacted at every level). |
| `BUXFER_API_BASE_URL` | no    | `https://www.buxfer.com/api`  | Buxfer REST API base URL. Override to point the client at a staging environment or local WireMock stub. Read by `BuxferClientConfig`.                                                  |

**Adding new environment variables:** every new env var must be added as a
commented example to `kotlin/.env.example`, following the existing comment
style. This keeps the file the single reference an operator needs when
configuring a fresh Kotlin-server deployment.

On startup, `BuxferClient` calls `POST /api/login` and stores the returned
token in memory. All subsequent tool calls inject this token.

**Do not log to stdout** — MCP communicates over stdio, and any stray stdout
output breaks the protocol. The default `logback.xml` enforces this by
configuring only a rolling file appender (no `ConsoleAppender`). `Main.kt`
writes fatal startup errors to `stderr` (which is not part of the MCP
transport) so the operator who launched the process sees them. Do not add
any `System.out.println` / `print` / `println` calls anywhere in production
code.

## Building & Running

```bash
# Build fat JAR (output at build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar)
gradle shadowJar

# Run directly (development; auto-loads ./.env from the kotlin/ cwd)
gradle run

# Run the fat JAR (auto-loads ./.env, or pass --env-file=<path>)
java -jar build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar
```

## Claude Desktop / Claude Code Integration

Two practical notes for whichever launcher you use:

- **Use an absolute Java path**, not the bare `java`. Claude Desktop / Claude Code launch the process with no guarantees about `PATH`, and ASDF shims need a `.tool-versions` file in the launch cwd to resolve the right version. The cleanest fix is the absolute path to the installed JDK (e.g. `~/.asdf/installs/java/<version>/bin/java`).
- **Pass `--env-file=/absolute/path/to/kotlin/.env`** (or omit it and rely on `./.env` if you can guarantee the launch cwd is `kotlin/`). Without one of those, the server will fail at startup because `BUXFER_EMAIL` / `BUXFER_PASSWORD` won't be found.

### Claude Desktop

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "buxfer": {
      "command": "/path/to/your/jdk/bin/java",
      "args": [
        "-jar",
        "/absolute/path/to/kotlin/build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar",
        "--env-file=/absolute/path/to/kotlin/.env"
      ]
    }
  }
}
```

### Claude Code

```bash
claude mcp add buxfer --scope local -- \
  /path/to/your/jdk/bin/java \
  -jar /absolute/path/to/kotlin/build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar \
  --env-file=/absolute/path/to/kotlin/.env
```

## Testing

### Running tests

```bash
# Unit + integration tests (no real credentials required)
gradle test
```

### Framework

| Library                   | Purpose                                                  |
|---------------------------|----------------------------------------------------------|
| JUnit 5 (`junit-jupiter`) | Test runner                                              |
| MockK                     | Kotlin-idiomatic mocking of `BuxferClient` in tool tests |
| Ktor `ktor-client-mock`   | `MockEngine` for HTTP-level tests of `BuxferClient`      |
| `kotlinx-coroutines-test` | `runTest` for testing `suspend` functions                |
| AssertJ + json-unit       | JSON-shape assertions on tool results and HTTP bodies    |
| WireMock                  | Real HTTP server for the integration suite               |
| `kotlin-sdk-testing`      | In-process MCP client/server pair for end-to-end tests   |

### Test structure

```
src/test/kotlin/com/buxfer/mcp/
├── EnvTest.kt                          # Env.load happy + failure paths
├── BuxferMcpServerTest.kt              # Tool registration / descriptions
├── BuxferMcpServerIntegrationTest.kt   # End-to-end via in-process MCP client + WireMock
├── TestFixtureLoader.kt                # Reads JSON from shared/test-fixtures/wiremock/__files/
├── api/
│   ├── BuxferClientTest.kt             # MockEngine + fixture JSON, deserialization checks
│   ├── BuxferClientErrorHandlingTest.kt# 4xx/5xx, IOException, timeout, non-OK status
│   ├── BuxferClientIntegrationTest.kt  # Real WireMock server, form-data assertions
│   └── BuxferClientConfigTest.kt       # Defaults + override behaviour
└── tools/
    ├── *ToolsTest.kt                   # One file per tool class — MockK-backed
    ├── McpToolTest.kt                  # Cross-tool error-result conventions
    └── JsonObjectExtensionsTest.kt     # require* / opt* helpers
```

### Shared fixtures

All tests load response JSON from `../shared/test-fixtures/wiremock/__files/` (path injected via the `fixtures.dir` system property in `build.gradle.kts`). The same fixture files are used by the TypeScript and Python implementations, so every language tests against an identical response contract. Fixture files are captured and anonymized by the language-agnostic `api-recordings/` module at the repo root — see `../shared/test-fixtures/CLAUDE.md` for the capture workflow and `../api-recordings/CLAUDE.md` for the anonymizer-fidelity contract.

### Testing expectations

Every class added to this project must have a corresponding test class. Specifically:
- Each `*Tools.kt` → `*ToolsTest.kt`: verify tool→client delegation and JSON output. Each test class covers the happy path and an error path (`isError = true`).
- `BuxferClient.kt` → `BuxferClientTest.kt`: verify deserialization of every endpoint using fixture files.
- New model fields added to `models/` should have a deserialization test in `BuxferClientTest`.

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`. That document is annotated inline with `> **Live API divergence**` callouts wherever the live Buxfer API behaves differently from the upstream documentation at https://www.buxfer.com/help/api — implementations should treat the callouts as ground truth.

## Implementation Notes

- **One class per file**: every class, data class, interface, object, or enum must live in its own `.kt` file named after it. No multi-class files.
- **No star imports**: always use explicit imports. Star imports (`import foo.*`) are forbidden.
- Remove all unused imports and organize them with the IDE's "Optimize Imports" feature before committing.
- `BuxferClient` must be thread-safe (token is set once, read many times — a simple `@Volatile` field suffices).
- All Ktor calls should be `suspend` functions; the MCP SDK handles coroutine dispatch.
- Use `kotlinx.serialization` annotations on model data classes (`@Serializable`, `@SerialName` where field names differ from Kotlin conventions).
- Return structured JSON strings from tools so Claude can parse them; prefer returning the raw API response object serialized to JSON.
- Error handling: wrap Ktor exceptions and Buxfer error responses into MCP `isError = true` tool results with a human-readable message.
