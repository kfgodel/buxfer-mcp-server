# buxfer-mcp-server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io) server that gives Claude access to your [Buxfer](https://www.buxfer.com) personal finance data. You can list accounts and transactions, add or edit expenses, track budgets, and more — all through natural conversation.

Three independent implementations are provided so you can run whichever matches your preferred runtime:

| Implementation | Directory | Status |
|---|---|---|
| Kotlin / JVM | [`kotlin/`](kotlin/) | Skeleton ready |
| TypeScript / Node | [`typescript/`](typescript/) | Not yet started |
| Python | [`python/`](python/) | Not yet started |

---

## Prerequisites

- [ASDF](https://asdf-vm.com/) for language and tool version management
- [Docker](https://www.docker.com/) to run the integration test mock server
- A [Buxfer](https://www.buxfer.com) account

---

## Getting started

### 1. Set up credentials

All modules share a single credentials file at the repo root:

```bash
cp .env.example .env
# open .env and fill in BUXFER_EMAIL and BUXFER_PASSWORD
```

`.env` is gitignored and never committed.

### 2. Refresh API recordings (do this first if the recordings are stale)

The `api-recordings/` module captures real Buxfer API responses, anonymizes them, and stores them as test fixtures. **All three implementations depend on these recordings** for integration testing — run this step before building or testing any implementation, and re-run it whenever the Buxfer API may have changed.

```bash
cd api-recordings
asdf plugin add hurl && asdf plugin add jq
asdf install
./run-capture.sh
```

A good rule of thumb: if the recordings are more than a few weeks old, or if you notice unexpected test failures, refresh them before investigating further.

See [`api-recordings/README.md`](api-recordings/CLAUDE.md) for details on the capture and anonymization workflow.

### 3. Start the mock server

```bash
cd api-recordings
docker compose up
```

WireMock starts on `http://localhost:8089` and serves the recorded responses. Keep it running while executing tests.

### 4. Run an implementation

Pick the implementation you want to use and follow its README:

- **Kotlin** → [`kotlin/CLAUDE.md`](kotlin/CLAUDE.md)
- **TypeScript** → [`typescript/CLAUDE.md`](typescript/CLAUDE.md) *(not yet implemented)*
- **Python** → [`python/CLAUDE.md`](python/CLAUDE.md) *(not yet implemented)*

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
