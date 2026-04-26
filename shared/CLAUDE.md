# Shared Resources

This directory contains language-agnostic reference material used by all three MCP server implementations.

## Contents

- `api-spec/buxfer-api.md` — Canonical Buxfer REST API specification. Single source of truth for tool definitions, parameter names, response shapes, and domain model fields across all implementations.
- `test-fixtures/` — Anonymized real Buxfer API responses used as the test contract for all three implementations. See `test-fixtures/CLAUDE.md` for the capture workflow and anonymization rules.

## Usage

When implementing or extending any of the three servers (Kotlin, TypeScript, Python):

1. Consult `api-spec/buxfer-api.md` first to understand the endpoint contract.
2. Map each Buxfer API endpoint to one MCP tool.
3. Name MCP tools and their parameters to match the Buxfer field names so Claude can correlate them intuitively.
4. Do **not** duplicate this spec into individual sub-project directories — always reference this file.

## Updating the Spec

If Buxfer adds or changes API endpoints, update `api-spec/buxfer-api.md` and then update all three server implementations accordingly.
