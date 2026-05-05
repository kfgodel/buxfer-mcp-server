# api-recordings

The authoritative source of real Buxfer API behaviour in this repository.

This module has two responsibilities:

1. **Capture** — use Hurl to make authenticated requests to the live Buxfer API, anonymize the responses with `jq`, and write the results to `../shared/test-fixtures/wiremock/__files/`.
2. **Mock server** — provide a WireMock configuration that serves those recorded responses over HTTP, so all three language implementations can run integration tests against a realistic Buxfer API without real credentials.

No application code lives here. This module is language-agnostic and has no dependency on the Kotlin, TypeScript, or Python implementations.

## Prerequisites

This module only requires:

```bash
asdf plugin add hurl
asdf plugin add jq
asdf install   # from this directory
```

Docker is required to run the WireMock mock server.

## Credentials

Credentials are loaded from a `.env` file at the **repository root**. Copy the example and fill in your values once:

```bash
cp ../.env.example ../.env
# then edit ../.env
```

The `.env` file is gitignored. You never need to export variables manually — `run-capture.sh` sources `../.env` automatically.

## Capturing new recordings

```bash
./run-capture.sh
```

This will:
1. Log in to Buxfer and obtain a session token.
2. Call every GET endpoint.
3. Create a test transaction, then edit and delete it (to capture write-endpoint responses).
4. Anonymize each response using the `jq` transforms in `anonymize/`.
5. Write results to `../shared/test-fixtures/wiremock/__files/`.

After running, review the diff carefully before committing:

```bash
git diff ../shared/test-fixtures/wiremock/__files/
```

The `upload_statement` endpoint requires a real statement file and must be captured manually — see `requests/upload_statement.hurl` for instructions.

## Running the mock server

```bash
docker compose up
```

WireMock starts on `http://localhost:8089`. All three language test suites should point their HTTP client at this address during integration tests.

The mock server:
- Returns the anonymized fixture JSON for every Buxfer endpoint.
- Returns `{"status":"OK","token":"test-mock-token"}` for the login endpoint.
- Ignores the `token` query parameter on all requests (any value is accepted).

## Directory layout

```
api-recordings/
├── .tool-versions          # hurl + jq versions
├── CLAUDE.md               # this file
├── compose.yml             # WireMock Docker Compose
├── run-capture.sh          # capture → anonymize → write fixtures
├── generate-wiremock.sh    # (re)generate WireMock mappings from fixture list
├── requests/               # one .hurl file per Buxfer API endpoint
│   ├── login.hurl
│   ├── accounts.hurl
│   ├── transactions.hurl
│   ├── transaction_add.hurl
│   ├── transaction_edit.hurl
│   ├── transaction_delete.hurl
│   ├── tags.hurl
│   ├── budgets.hurl
│   ├── reminders.hurl
│   ├── groups.hurl
│   ├── contacts.hurl
│   ├── loans.hurl
│   └── upload_statement.hurl
└── anonymize/              # one .jq transform per endpoint response
    ├── accounts.jq
    ├── transactions.jq
    ├── transaction_add.jq
    ├── transaction_edit.jq
    ├── transaction_delete.jq
    ├── tags.jq
    ├── budgets.jq
    ├── reminders.jq
    ├── groups.jq
    ├── contacts.jq
    ├── loans.jq
    └── upload_statement.jq
```

## Adding a new endpoint

1. Add a `requests/<endpoint>.hurl` file.
2. Add an `anonymize/<endpoint>.jq` transform.
3. Add the endpoint to the `ENDPOINTS` list in `run-capture.sh`.
4. Run `./generate-wiremock.sh` to update the WireMock mappings.
5. Run `./run-capture.sh` to capture the new fixture.
