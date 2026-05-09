# Buxfer MCP Server — Python Implementation

## Status: Not yet started

This directory will contain the Python implementation of the Buxfer MCP server.

## Prerequisites

- Python 3.12 (managed via ASDF):

```bash
asdf plugin add python  # if not already added
asdf install
```

- [pyright](https://github.com/microsoft/pyright) for LSP support in Claude Code:

```bash
# 1. Install the language server binary
npm install -g pyright

# 2. Install and enable the Claude Code plugin (run once per machine)
claude plugin install pyright-lsp@claude-plugins-official
claude plugin enable pyright-lsp
```

The LSP project configuration is already committed at `.claude/settings.json`.

To verify it's working, check `~/.claude/debug/` for a line like:
```
LSP server plugin:pyright-lsp:python initialized
```

## Code Intelligence — LSP First

**Always use the LSP tool as the primary way to explore code.** Before guessing an import path, checking a type, or navigating to a definition, use LSP:

- `workspaceSymbol` — find any class, function, or type by name across the whole project and indexed deps
- `goToDefinition` — jump to where a symbol is defined (works for SDK types once the project is set up)
- `hover` — inspect type signatures and documentation in place
- `findReferences` / `documentSymbol` — explore usages or the structure of a file

**When LSP cannot resolve a symbol, ask the user** rather than resorting to `site-packages` inspection or other workarounds.

## Running Commands

Always use ASDF shims so the correct Python version from `.tool-versions` is used:

```bash
# From python/
PATH="$HOME/.asdf/shims:$PATH" python -m buxfer_mcp
PATH="$HOME/.asdf/shims:$PATH" python -m pytest
PATH="$HOME/.asdf/shims:$PATH" uv run python ...
```

## Planned Stack

| Component | Library |
|-----------|---------|
| MCP protocol | `mcp` (official Python SDK: `pip install mcp`) |
| HTTP client | `httpx` (async) |
| Serialization | `pydantic` v2 models |
| Runtime | Python 3.12 (managed via ASDF) |
| Package manager | `uv` or `pip` with `pyproject.toml` |

## Planned Structure (to be scaffolded)

```
python/
├── pyproject.toml
├── .tool-versions           # python 3.12.7
└── src/
    └── buxfer_mcp/
        ├── __main__.py      # Entry point
        ├── server.py        # MCP server bootstrap + tool registration
        ├── client.py        # httpx-based HTTP client, token management
        ├── models/          # Pydantic models for all domain objects
        │   ├── transaction.py
        │   ├── account.py
        │   └── ...
        └── tools/           # Tool handler functions
            ├── transaction_tools.py
            ├── account_tools.py
            └── lookup_tools.py
```

## Configuration

Credentials are read from environment variables:

| Variable         | Description              |
|------------------|--------------------------|
| `BUXFER_EMAIL`   | Buxfer account email     |
| `BUXFER_PASSWORD`| Buxfer account password  |

Each module keeps its own `.env`. When this implementation is
scaffolded, follow the same pattern as `kotlin/`: ship a
`python/.env.example`, copy it to `python/.env`, and have the entry
point load it on startup. There is no shared repo-root `.env`.

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`.

## Implementation Notes

- Use the `mcp` SDK's `stdio_server` context manager for transport.
- All tool handlers are `async def` functions.
- Never `print()` to stdout — MCP uses it for protocol messages. Use `sys.stderr` for debug output.
- Token management: call login on startup, store as module-level variable, inject into every request.
- Use Pydantic models both for API response deserialization and as MCP tool input schemas.
