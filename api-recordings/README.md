# api-recordings

Captures real Buxfer API responses, anonymizes them, and provides a WireMock mock server so all three MCP server implementations can run integration tests without hitting the live API.

## When to refresh

Refresh the recordings before running any implementation's tests if:
- You have just cloned the repository and fixtures are empty placeholders.
- The recordings are more than a few months old.
- You see unexpected test failures that might indicate an API response shape change.

## Setup

```bash
cd api-recordings
asdf plugin add hurl && asdf plugin add jq   # first time only
asdf install
```

Credentials are loaded automatically from `api-recordings/.env`. Copy `.env.example` to `.env` and fill in `BUXFER_EMAIL` / `BUXFER_PASSWORD` before running.

## Refreshing recordings

```bash
./run-capture.sh
```

This logs in to Buxfer, calls every endpoint, anonymizes each response, and writes the results to `../shared/test-fixtures/wiremock/__files/`. Review the diff before committing:

```bash
git diff ../shared/test-fixtures/wiremock/__files/
```

> `upload_statement` must be captured manually — see `requests/upload_statement.hurl` for instructions.

## Starting the mock server

```bash
docker compose up
```

WireMock starts on `http://localhost:8089` and serves the recorded responses. Keep it running while executing tests in any of the language implementations. The login endpoint returns a fixed token (`test-mock-token`) so no real credentials are needed during tests.

## Adding a new endpoint

1. Add `requests/<endpoint>.hurl`
2. Add `anonymize/<endpoint>.jq`
3. Add the endpoint to `run-capture.sh`
4. Run `./generate-wiremock.sh` to regenerate the WireMock mappings
5. Run `./run-capture.sh` to capture the new fixture
