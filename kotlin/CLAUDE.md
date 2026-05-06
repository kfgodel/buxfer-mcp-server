# Buxfer MCP Server — Kotlin Implementation

## Overview

A Kotlin/JVM implementation of the Buxfer MCP server. Uses the official [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) and [Ktor](https://ktor.io/) for HTTP calls to the Buxfer REST API.

## Current improvement work

A multi-session refactor is in flight. Phases 1–5 of [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) — AssertJ migration, Logback rolling-file logging, configurable base URL, server-bootstrap unit tests, integration tests with WireMock + ChannelTransport, and a bottom-up code review of every production layer (Models → API client → Tools → Server → Main) — are complete. **All 59 tests green.**

Three independent follow-ups remain:

1. **Resilience to intermittent server failures** — see [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) §"Resilience to intermittent server failures". `BuxferClient` retry on transient failures, token refresh on 401, wrapping `IOException` / `HttpRequestTimeoutException` from Ktor as `BuxferApiException`.
2. **Challenge the fixed-model-class approach** — see [IMPROVEMENT_PLAN.md](IMPROVEMENT_PLAN.md) §"Challenge the fixed-model-class approach". Phase 1 inventory found that no response-model field is read programmatically. Should we drop the 14 typed response models in favor of a `JsonObject` pass-through, since Claude (the consumer) parses JSON itself?
3. **Test-suite gaps and cleanups** — see [TEST_REVIEW.md](TEST_REVIEW.md). Bottom-up review of the test suite, ranked by severity. Highest-value items: direct tests for `JsonObjectExtensions` and `mcpTool` helpers; coverage for `TransactionTools` arg-validation paths.

Items are independent — pick any to start. After all three close, Phase 5 is genuinely done.

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

### SDK gotchas (things `hover` won't tell you)

- **`ServerCapabilities()` no-args defaults `tools = null`**, which makes `Server.addTool(...)` throw `IllegalStateException: Server does not support tools capability`. Pass `ServerCapabilities(tools = ServerCapabilities.Tools())` to actually enable tool registration.
- **No `InMemoryTransport` ships with the SDK** (verified in 0.11.1). A test that wants in-process server↔client wiring has to roll its own by extending `AbstractTransport` (override `start`/`send`/`close`; dispatch inbound messages by invoking `_onMessage`).

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
    ├── Main.kt                         # Entry point — wires up and starts MCP server
    ├── BuxferMcpServer.kt              # Server bootstrap, tool registration
    ├── api/
    │   ├── BuxferClient.kt             # Ktor HTTP client, token management
    │   └── models/                     # Kotlinx.serialization data classes
    │       ├── Transaction.kt
    │       ├── Account.kt
    │       ├── Tag.kt
    │       ├── Budget.kt
    │       ├── Reminder.kt
    │       ├── Group.kt
    │       ├── Contact.kt
    │       └── Loan.kt
    └── tools/
        ├── TransactionTools.kt         # MCP tools: list/add/edit/delete/upload
        ├── AccountTools.kt             # MCP tool: list accounts
        ├── TagTools.kt                 # MCP tool: list tags
        ├── BudgetTools.kt              # MCP tool: list budgets
        ├── ReminderTools.kt            # MCP tool: list reminders
        ├── GroupTools.kt               # MCP tool: list groups
        ├── ContactTools.kt             # MCP tool: list contacts
        └── LoanTools.kt                # MCP tool: list loans
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
| `ch.qos.logback:logback-classic`                   | SLF4J binding; rolling-file logging — see `src/main/resources/logback.xml`. Never logs to stdout (the MCP transport); writes to `./logs/server.log` by default. Pulls in `slf4j-api` transitively. |

## Configuration

The server reads credentials and operational settings from environment variables:

| Variable           | Required | Default   | Description                                                                                                                                                                            |
|--------------------|----------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BUXFER_EMAIL`     | yes      | —         | Buxfer account email. **Never logged** (PII / account identifier).                                                                                                                     |
| `BUXFER_PASSWORD`  | yes      | —         | Buxfer account password. **Never logged**.                                                                                                                                             |
| `BUXFER_LOG_DIR`      | no       | `./logs`                       | Directory the rolling log appender writes to (relative to the server's working directory). Resolved by `logback.xml`.                                                                  |
| `BUXFER_LOG_LEVEL`    | no       | `INFO`                         | Logback root level. Lowering to `DEBUG` enables HTTP request/response logging in `BuxferClient` and per-arg value logging in tools (the always-redacted set — password, token, statement — stays redacted at every level). |
| `BUXFER_API_BASE_URL` | no       | `https://www.buxfer.com/api`   | Buxfer REST API base URL. Override to point the client at a staging environment or local WireMock stub. Read by `BuxferClientConfig`. |

**Adding new environment variables:** every new env var must be added as a commented example to the root `.env.example` file, following the existing comment style. This keeps the file the single reference an operator needs when configuring a fresh deployment.

For local development, set these in the root `.env` file and load it before running:

```bash
# from kotlin/
set -a && source ../.env && set +a
gradle run
```

On startup, `BuxferClient` calls `POST /api/login` and stores the returned token in memory. All subsequent tool calls inject this token.

**Do not log to stdout** — MCP communicates over stdio, and any stray stdout output breaks the protocol. The default `logback.xml` enforces this by configuring only a rolling file appender (no `ConsoleAppender`). `Main.kt` writes fatal startup errors to `stderr` (which is not part of the MCP transport) so the operator who launched the process sees them; the same messages also reach the log file via `log.error(...)`. Do not add any further `System.out.println` / `print` / `println` calls anywhere in production code.

## Building & Running

```bash
# Build fat JAR
gradle shadowJar

# Run directly (development)
gradle run

# Run the fat JAR
java -jar build/libs/buxfer-mcp-server-all.jar
```

## Claude Desktop Integration

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "buxfer": {
      "command": "java",
      "args": ["-jar", "/path/to/kotlin/build/libs/buxfer-mcp-server-all.jar"],
      "env": {
        "BUXFER_EMAIL": "your@email.com",
        "BUXFER_PASSWORD": "yourpassword"
      }
    }
  }
}
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

### Test structure

```
src/test/kotlin/com/buxfer/mcp/
├── TestFixtureLoader.kt          # Loads JSON from shared/test-fixtures/wiremock/__files/
├── api/
│   └── BuxferClientTest.kt       # HTTP-level: MockEngine + fixture JSON
└── tools/
    ├── TransactionToolsTest.kt   # Unit: MockK BuxferClient
    ├── AccountToolsTest.kt
    └── LookupToolsTest.kt
```

### Shared fixtures

All tests load response JSON from `../shared/test-fixtures/wiremock/__files/` (path injected via the `fixtures.dir` system property in `build.gradle.kts`). The same fixture files are used by the TypeScript and Python implementations, so every language tests against an identical response contract. Fixture files are captured and anonymized by the language-agnostic `api-recordings/` module at the repo root — see `../shared/test-fixtures/CLAUDE.md` for the capture workflow.

### Testing expectations

Every class added to this project must have a corresponding test class. Specifically:
- Each `*Tools.kt` → `*ToolsTest.kt`: verify tool→client delegation and JSON output. Each test class covers the happy path and an error path (`isError = true`).
- `BuxferClient.kt` → `BuxferClientTest.kt`: verify deserialization of every endpoint using fixture files.
- New model fields added to `models/` should have a deserialization test in `BuxferClientTest`.

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`. Implement each MCP tool to match the parameter names and types specified there.

## Implementation Notes

- **One class per file**: every class, data class, interface, object, or enum must live in its own `.kt` file named after it. No multi-class files.
- **No star imports**: always use explicit imports. Star imports (`import foo.*`) are forbidden.
- Remove all unused imports and organize them with the IDE's "Optimize Imports" feature before committing.
- `BuxferClient` must be thread-safe (token is set once, read many times — a simple `@Volatile` field suffices).
- All Ktor calls should be `suspend` functions; the MCP SDK handles coroutine dispatch.
- Use `kotlinx.serialization` annotations on model data classes (`@Serializable`, `@SerialName` where field names differ from Kotlin conventions).
- Return structured JSON strings from tools so Claude can parse them; prefer returning the raw API response object serialized to JSON.
- Error handling: wrap Ktor exceptions and Buxfer error responses into MCP `isError = true` tool results with a human-readable message.
