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

[ASDF](https://asdf-vm.com/) is used for all runtime versions. Each sub-project has its own `.tool-versions` file. From the repo root, `asdf install` will install all declared versions.

Required ASDF plugins:
- `asdf plugin add java`
- `asdf plugin add nodejs`
- `asdf plugin add python`

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
