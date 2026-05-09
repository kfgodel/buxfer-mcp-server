# buxfer-mcp-server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that gives Claude access to your [Buxfer](https://www.buxfer.com) personal finance data. You can list accounts and transactions, add or edit expenses, track budgets, and more — all through natural conversation.

Three independent implementations are planned so you can run whichever matches your preferred runtime:

| Implementation | Directory | Status |
|---|---|---|
| Kotlin / JVM | [`kotlin/`](kotlin/) | **Working** — live-verified against the real Buxfer API |
| TypeScript / Node | [`typescript/`](typescript/) | Not yet started |
| Python | [`python/`](python/) | Not yet started |

---

## Getting started

Pick the implementation you want and follow its README:

- **Kotlin** → **[`kotlin/README.md`](kotlin/README.md)** — five-step setup walkthrough (build the JAR, register the MCP with Claude Code or Claude Desktop, try it).

Each module is standalone — it has its own `.env.example` to copy and fill in with `BUXFER_EMAIL` / `BUXFER_PASSWORD`, so you only need to set up credentials for the module(s) you're actually using. The implementation's README walks you through it.

---

## Repository layout

```
buxfer-mcp-server/
├── .env.example          # credential template — copy to .env
├── api-recordings/       # Hurl requests, jq anonymization, WireMock config
├── shared/
│   ├── api-spec/         # Buxfer REST API specification (source of truth)
│   └── test-fixtures/    # Anonymized API responses + WireMock stubs
├── kotlin/               # Kotlin/JVM MCP server
├── typescript/           # TypeScript/Node MCP server
└── python/               # Python MCP server
```

---

## How it works

Each MCP server exposes the Buxfer API as a set of tools that Claude can call. When you connect Claude Desktop or Claude Code to one of these servers, Claude can answer questions like:

- *"What did I spend on groceries last month?"*
- *"Add a $45 expense to my Visa for lunch today, tag it Dining."*
- *"How much is left in my Food budget?"*

Authentication happens once on startup: the server logs in with your credentials, stores the session token in memory, and injects it into every API call automatically.
