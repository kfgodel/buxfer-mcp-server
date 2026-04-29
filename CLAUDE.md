# Buxfer MCP Server — Monorepo

## Project Purpose

A Model Context Protocol (MCP) server that exposes [Buxfer](https://www.buxfer.com) personal finance data and actions to Claude. Three independent implementations exist so users can run whichever matches their preferred runtime.

## Repository Layout

```
buxfer-mcp-server/
├── api-recordings/  # Recorded API interactions for testing and documentation
├── shared/          # Language-agnostic API spec and shared test fixtures
├── kotlin/          # Kotlin/JVM implementation (Gradle, MCP Kotlin SDK)
├── typescript/      # TypeScript/Node implementation (not yet started)
└── python/          # Python implementation (not yet started)
```

## Credentials

Buxfer credentials are stored in a single `.env` file at the repository root and shared across all modules:

```bash
cp .env.example .env
# edit .env — add BUXFER_EMAIL and BUXFER_PASSWORD
```

`.env` is gitignored. Each module that needs credentials loads it automatically — you never need to export variables by hand. See `.env.example` for the full variable list.

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

## Cross-Language Implementation Conventions

These rules apply to all three language implementations and must be followed consistently so the servers behave identically and the codebases stay navigable.

### Tool naming

MCP tool names follow the pattern `buxfer_<verb>_<resource>` (e.g. `buxfer_list_accounts`, `buxfer_add_transaction`). The full list is defined in `shared/api-spec/buxfer-api.md` and must not diverge between implementations.

### Tool class organization

**One tool class per resource.** Each Buxfer domain resource gets its own class containing all operations on that resource:

| Resource    | Tool class         | MCP tools it handles                                                           |
|-------------|--------------------|--------------------------------------------------------------------------------|
| accounts    | `AccountTools`     | `buxfer_list_accounts`                                                         |
| transactions| `TransactionTools` | `buxfer_list_transactions`, `buxfer_add_transaction`, `buxfer_edit_transaction`, `buxfer_delete_transaction`, `buxfer_upload_statement` |
| tags        | `TagTools`         | `buxfer_list_tags`                                                             |
| budgets     | `BudgetTools`      | `buxfer_list_budgets`                                                          |
| reminders   | `ReminderTools`    | `buxfer_list_reminders`                                                        |
| groups      | `GroupTools`       | `buxfer_list_groups`                                                           |
| contacts    | `ContactTools`     | `buxfer_list_contacts`                                                         |
| loans       | `LoanTools`        | `buxfer_list_loans`                                                            |

Do not bundle unrelated resources into a single class (e.g. a catch-all `LookupTools`).

### One class per file

Every class (model, tool, or otherwise) lives in its own file named after it. No multi-class files.

## Shared Resources

`shared/api-spec/buxfer-api.md` is the single authoritative reference for:
- Authentication flow
- Every endpoint (method, path, parameters, response shape)
- Enum values for `type`, `status`, `period`, etc.
- Data models for all domain objects

When implementing any language version, derive tool definitions and model classes solely from that document.

## Running an Implementation

Each sub-project is self-contained. See the `CLAUDE.md` inside each directory for build, run, and configuration instructions specific to that language.

## Running Commands

Always use ASDF shims to run language-specific tools. Do not set `JAVA_HOME` or `PATH` manually — instead put `~/.asdf/shims` on `PATH` so ASDF resolves the correct binary from the working directory's `.tool-versions`:

```bash
# From the relevant sub-project directory
PATH="$HOME/.asdf/shims:$PATH" gradle test
PATH="$HOME/.asdf/shims:$PATH" gradle build
```

## Code Intelligence — LSP First

**Always use the LSP tool before any other approach** when you need to:
- Discover the correct import path for a class or symbol
- Check a type signature or method signature
- Navigate to a definition or find references

Use `workspaceSymbol` to search for a symbol by name across all indexed files (including SDK dependencies once the project compiles). Use `goToDefinition` to jump to a symbol's definition. Use `hover` to inspect type information.

Only fall back to other approaches (asking the user, running a compile to read error output) if the LSP cannot resolve the symbol — which typically happens when the project has not compiled yet and the LSP has not indexed external dependencies.
