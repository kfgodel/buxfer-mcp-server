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

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`. Implement each MCP tool to match the parameter names and types specified there.

## Implementation Notes

- `BuxferClient` must be thread-safe (token is set once, read many times — a simple `@Volatile` field suffices).
- All Ktor calls should be `suspend` functions; the MCP SDK handles coroutine dispatch.
- Use `kotlinx.serialization` annotations on model data classes (`@Serializable`, `@SerialName` where field names differ from Kotlin conventions).
- Return structured JSON strings from tools so Claude can parse them; prefer returning the raw API response object serialized to JSON.
- Error handling: wrap Ktor exceptions and Buxfer error responses into MCP `isError = true` tool results with a human-readable message.
