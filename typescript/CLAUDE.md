# Buxfer MCP Server — TypeScript Implementation

## Status: Not yet started

This directory will contain the TypeScript/Node.js implementation of the Buxfer MCP server.

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

For local development, load from the root `.env` file:

```bash
# from typescript/
set -a && source ../.env && set +a
npm start
```

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`.

## Implementation Notes

- Use the stdio transport from `@modelcontextprotocol/sdk/server/stdio.js`.
- Never `console.log` — MCP uses stdout for protocol messages. Use `console.error` for debug output.
- Token management: call login on startup, store in module scope, inject into every API call.
- Define Zod schemas for all tool input parameters (the MCP SDK can use them directly for `inputSchema`).
