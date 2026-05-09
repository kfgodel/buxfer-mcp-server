# Buxfer MCP Server — TypeScript Implementation

## Status: Not yet started

This directory will contain the TypeScript/Node.js implementation of the Buxfer MCP server.

## Prerequisites

- Node.js 22 (managed via ASDF):

```bash
asdf plugin add nodejs  # if not already added
asdf install
```

- [typescript-language-server](https://github.com/typescript-language-tools/typescript-language-server) for LSP support in Claude Code:

```bash
# 1. Install the language server binary
npm install -g typescript typescript-language-server

# 2. Install and enable the Claude Code plugin (run once per machine)
claude plugin install typescript-lsp@claude-plugins-official
claude plugin enable typescript-lsp
```

The LSP project configuration is already committed at `.claude/settings.json`.

To verify it's working, check `~/.claude/debug/` for a line like:
```
LSP server plugin:typescript-lsp:typescript initialized
```

## Code Intelligence — LSP First

**Always use the LSP tool as the primary way to explore code.** Before guessing an import path, checking a type, or navigating to a definition, use LSP:

- `workspaceSymbol` — find any class, function, or type by name across the whole project and indexed deps
- `goToDefinition` — jump to where a symbol is defined (works for SDK types once the project compiles)
- `hover` — inspect type signatures and documentation in place
- `findReferences` / `documentSymbol` — explore usages or the structure of a file

**When LSP cannot resolve a symbol, ask the user** rather than resorting to `node_modules` inspection or other workarounds.

## Running Commands

Always use ASDF shims so the correct Node.js version from `.tool-versions` is used:

```bash
# From typescript/
PATH="$HOME/.asdf/shims:$PATH" npm test
PATH="$HOME/.asdf/shims:$PATH" npm run build
PATH="$HOME/.asdf/shims:$PATH" npx tsc --noEmit
```

## Planned Stack

| Component | Library |
|-----------|---------|
| MCP protocol | `@modelcontextprotocol/sdk` |
| HTTP client | `axios` or native `fetch` |
| Serialization | TypeScript interfaces + `zod` for validation |
| Runtime | Node.js 22 (managed via ASDF) |
| Build | `tsc` + `tsx` for development |

## Planned Structure (to be scaffolded)

```
typescript/
├── package.json
├── tsconfig.json
├── .tool-versions           # nodejs 22.11.0
└── src/
    ├── index.ts             # Entry point
    ├── server.ts            # MCP server bootstrap + tool registration
    ├── buxferClient.ts      # fetch-based HTTP client, token management
    ├── models/              # TypeScript interfaces for all domain objects
    │   ├── transaction.ts
    │   ├── account.ts
    │   └── ...
    └── tools/               # Tool handler functions
        ├── transactionTools.ts
        ├── accountTools.ts
        └── lookupTools.ts
```

## Configuration

Credentials are read from environment variables:

| Variable         | Description              |
|------------------|--------------------------|
| `BUXFER_EMAIL`   | Buxfer account email     |
| `BUXFER_PASSWORD`| Buxfer account password  |

Each module keeps its own `.env`. When this implementation is
scaffolded, follow the same pattern as `kotlin/`: ship a
`typescript/.env.example`, copy it to `typescript/.env`, and have the
entry point load it on startup. There is no shared repo-root `.env`.

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`.

## Implementation Notes

- Use the stdio transport from `@modelcontextprotocol/sdk/server/stdio.js`.
- Never `console.log` — MCP uses stdout for protocol messages. Use `console.error` for debug output.
- Token management: call login on startup, store in module scope, inject into every API call.
- Define Zod schemas for all tool input parameters (the MCP SDK can use them directly for `inputSchema`).
