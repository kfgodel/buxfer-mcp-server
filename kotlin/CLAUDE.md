# Buxfer MCP Server — Kotlin Implementation

## Overview

A Kotlin/JVM implementation of the Buxfer MCP server. Uses the official [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) and [Ktor](https://ktor.io/) for HTTP calls to the Buxfer REST API.

## Prerequisites

- Java 21 and Gradle 9.4.1, both managed via [ASDF](https://asdf-vm.com/).

```bash
# From the kotlin/ directory
asdf plugin add java    # if not already added
asdf plugin add gradle  # if not already added
asdf install            # installs versions declared in .tool-versions
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
        └── LookupTools.kt              # MCP tools: tags, budgets, reminders, groups, contacts, loans
```

## Key Dependencies

See `build.gradle.kts` for pinned versions.

| Library | Purpose |
|---|---|
| `io.modelcontextprotocol:kotlin-sdk` (0.11.1) | MCP server protocol (stdio transport) |
| `io.ktor:ktor-client-cio` | Async HTTP client for Buxfer API calls |
| `io.ktor:ktor-client-content-negotiation` | JSON content negotiation |
| `io.ktor:ktor-serialization-kotlinx-json` | JSON serialization via Ktor |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | Data class serialization |
| `org.slf4j:slf4j-nop` | Suppress SLF4J warnings on stdio (important: logging to stdout breaks MCP) |

## Configuration

The server reads credentials from environment variables:

| Variable         | Description              |
|------------------|--------------------------|
| `BUXFER_EMAIL`   | Buxfer account email     |
| `BUXFER_PASSWORD`| Buxfer account password  |

On startup, `BuxferClient` calls `POST /api/login` and stores the returned token in memory. All subsequent tool calls inject this token.

**Do not log to stdout** — MCP communicates over stdio, and any stray output breaks the protocol. All logging must go to stderr or be suppressed.

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

Fixture capture is handled by the language-agnostic `api-recordings/` module — not Kotlin code. Run `./run-capture.sh` there first to populate `shared/test-fixtures/responses/` before running tests.

### Framework

| Library | Purpose |
|---|---|
| JUnit 5 (`junit-jupiter`) | Test runner |
| MockK | Kotlin-idiomatic mocking of `BuxferClient` in tool tests |
| Ktor `ktor-client-mock` | `MockEngine` for HTTP-level tests of `BuxferClient` |
| `kotlinx-coroutines-test` | `runTest` for testing `suspend` functions |

### Test structure

```
src/test/kotlin/com/buxfer/mcp/
├── TestFixtureLoader.kt          # Loads JSON from shared/test-fixtures/responses/
├── api/
│   └── BuxferClientTest.kt       # HTTP-level: MockEngine + fixture JSON
├── tools/
│   ├── TransactionToolsTest.kt   # Unit: MockK BuxferClient
│   ├── AccountToolsTest.kt
│   └── LookupToolsTest.kt
└── capture/
    └── CaptureFixtures.kt        # @Tag("capture") — hits real API, writes fixtures
```

### Shared fixtures

All tests load response JSON from `../shared/test-fixtures/responses/` (path injected via the `fixtures.dir` system property in `build.gradle.kts`). The same fixture files are used by the TypeScript and Python implementations, so every language tests against an identical response contract.

See `../shared/test-fixtures/CLAUDE.md` for the anonymization rules and capture workflow.

### Testing expectations

Every class added to this project must have a corresponding test class. Specifically:
- Each `*Tools.kt` → `*ToolsTest.kt`: verify tool→client delegation and JSON output.
- `BuxferClient.kt` → `BuxferClientTest.kt`: verify deserialization of every endpoint using fixture files.
- New model fields added to `models/` should have a deserialization test in `BuxferClientTest`.

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`. Implement each MCP tool to match the parameter names and types specified there.

## Implementation Notes

- `BuxferClient` must be thread-safe (token is set once, read many times — a simple `@Volatile` field suffices).
- All Ktor calls should be `suspend` functions; the MCP SDK handles coroutine dispatch.
- Use `kotlinx.serialization` annotations on model data classes (`@Serializable`, `@SerialName` where field names differ from Kotlin conventions).
- Return structured JSON strings from tools so Claude can parse them; prefer returning the raw API response object serialized to JSON.
- Error handling: wrap Ktor exceptions and Buxfer error responses into MCP `isError = true` tool results with a human-readable message.
