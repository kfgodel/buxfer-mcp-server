# Buxfer MCP Server — Monorepo

## Project Purpose

A Model Context Protocol (MCP) server that exposes [Buxfer](https://www.buxfer.com) personal finance data and actions to Claude. Three independent implementations exist so users can run whichever matches their preferred runtime.

## Repository Layout

```
buxfer-mcp-server/
├── shared/          # Language-agnostic API spec and data model docs
├── kotlin/          # Kotlin/JVM implementation (Gradle, MCP Kotlin SDK)
├── typescript/      # TypeScript/Node implementation (not yet started)
└── python/          # Python implementation (not yet started)
```

## Language Version Management

[ASDF](https://asdf-vm.com/) is used for all runtime versions. Each sub-project is fully self-contained: its own `.tool-versions` declares exactly the tools it needs. There is no root-level `.tool-versions`.

To set up a sub-project, `cd` into it and run `asdf install`. Required plugins per project:

| Project      | Plugins needed                         |
|--------------|----------------------------------------|
| `kotlin/`    | `asdf plugin add java gradle`          |
| `typescript/`| `asdf plugin add nodejs`               |
| `python/`    | `asdf plugin add python`               |

## Implementations Status

| Language   | Status           | Entry point           |
|------------|------------------|-----------------------|
| Kotlin     | Skeleton ready   | `kotlin/`             |
| TypeScript | Not started      | `typescript/`         |
| Python     | Not started      | `python/`             |

## MCP Protocol

All three servers expose the same set of MCP **tools** (Claude-callable functions) mapping 1-to-1 to the Buxfer API surface described in `shared/api-spec/buxfer-api.md`.

Servers communicate via **stdio** transport (standard for local MCP servers used with Claude Desktop / Claude Code).

## Shared Resources

`shared/api-spec/buxfer-api.md` is the single authoritative reference for:
- Authentication flow
- Every endpoint (method, path, parameters, response shape)
- Enum values for `type`, `status`, `period`, etc.
- Data models for all domain objects

When implementing any language version, derive tool definitions and model classes solely from that document.

## Running an Implementation

Each sub-project is self-contained. See the `CLAUDE.md` inside each directory for build, run, and configuration instructions specific to that language.
