# Buxfer MCP Server — Python Implementation

## Status: Not yet started

This directory will contain the Python implementation of the Buxfer MCP server.

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

For local development, load from the root `.env` file:

```bash
# from python/
set -a && source ../.env && set +a
python -m buxfer_mcp
```

## API Reference

All tool contracts are defined in `../shared/api-spec/buxfer-api.md`.

## Implementation Notes

- Use the `mcp` SDK's `stdio_server` context manager for transport.
- All tool handlers are `async def` functions.
- Never `print()` to stdout — MCP uses it for protocol messages. Use `sys.stderr` for debug output.
- Token management: call login on startup, store as module-level variable, inject into every request.
- Use Pydantic models both for API response deserialization and as MCP tool input schemas.
