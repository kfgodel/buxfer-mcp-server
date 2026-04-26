# Shared Test Fixtures

Anonymized, realistic Buxfer API responses used as the test contract for all three MCP server implementations (Kotlin, TypeScript, Python).

## Why shared fixtures?

- **No real API hits** during normal test runs — credentials not required.
- **Consistency** — all three implementations test against identical response shapes, catching any deserialization divergence.
- **Realistic** — fixtures are captured from a real Buxfer account then anonymized, so edge cases in the actual API format are preserved.

## Directory layout

```
shared/test-fixtures/
├── CLAUDE.md               # this file
├── responses/              # one JSON file per Buxfer API endpoint
│   ├── accounts.json
│   ├── transactions.json
│   ├── transaction_add.json
│   ├── transaction_edit.json
│   ├── transaction_delete.json
│   ├── upload_statement.json
│   ├── tags.json
│   ├── budgets.json
│   ├── reminders.json
│   ├── groups.json
│   ├── contacts.json
│   └── loans.json
└── capture/                # scripts/utilities to refresh fixtures from a real account
    └── (populated when Kotlin implementation is complete)
```

## Fixture format

Each file contains the verbatim JSON body that the Buxfer API returns for the corresponding endpoint, with personal data replaced according to the anonymization rules below. The `status: "OK"` wrapper is preserved because implementations must parse it.

## Anonymization rules

When capturing or updating fixtures, apply these substitutions:

| Data type       | Replace with                              |
|-----------------|-------------------------------------------|
| Real names      | `"Test User"`, `"Contact One"`, `"Roommate One"`, etc. |
| Real emails     | `contact.one@example.com`, `contact.two@example.com`   |
| Bank names      | `"Test Bank"`, `"Other Test Bank"`        |
| Account names   | `"Test Checking"`, `"Test Savings"`, `"Test Visa"` |
| Real amounts    | Round numbers that preserve sign and rough magnitude   |
| Real dates      | Shift to a fixed recent period (e.g. 2025-01-xx)      |
| IDs             | Replace with sequential test IDs (1001, 2001, …)      |
| Descriptions    | Generic labels (`"Grocery Store"`, `"Monthly Salary"`) |

The goal is that the fixture is **structurally identical** to a real response but contains no identifiable personal data.

## Updating fixtures (capturing from a real account)

Fixtures should be refreshed whenever the Buxfer API changes its response shape or when new fields are discovered. The capture utility lives in the Kotlin project and can be run from the `kotlin/` directory:

```bash
# Requires real credentials — never commit these
export BUXFER_EMAIL="your@email.com"
export BUXFER_PASSWORD="yourpassword"

# Run only the capture test tag (excluded from normal test runs)
gradle test -Dinclude.tags=capture
```

This will:
1. Log in to Buxfer and obtain a real token.
2. Call every GET endpoint and a set of representative POST endpoints with test data.
3. Anonymize all response bodies using the rules above.
4. Overwrite the files in `shared/test-fixtures/responses/`.

Review the diff carefully before committing — ensure no real personal data was missed.

## Using fixtures in tests

### Kotlin

`TestFixtureLoader.kt` (in `src/test/kotlin/com/buxfer/mcp/`) loads fixtures by name:

```kotlin
val json = TestFixtureLoader.load("accounts") // reads accounts.json
```

The path is injected via the `fixtures.dir` system property set in `build.gradle.kts`:

```kotlin
systemProperty("fixtures.dir", "${project.projectDir}/../shared/test-fixtures/responses")
```

### TypeScript (planned)

Read fixture files using `fs.readFileSync` relative to the project root, or load them as imported JSON modules.

### Python (planned)

Use `pathlib.Path(__file__).parent / "../../../../shared/test-fixtures/responses"` or set the path via an environment variable in `pyproject.toml` test config.
