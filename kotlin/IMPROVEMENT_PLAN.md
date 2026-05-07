# Kotlin Module — Improvement Plan

Living document for in-flight work. Closed phases and historical context are pruned periodically; the git log is the source of truth for past commits. As of **2026-05-07**, the plan that opened this file is functionally complete: 92 tests green, no in-flight work. Three follow-ups remain, all wait-for-signal.

---

## Conventions

These crystallised during the multi-session refactor and apply to every future change. Mirror them when adding new endpoints, models, or tests.

### Three-tier nullability

Schemas in `kotlin/src/main/kotlin/com/buxfer/mcp/api/models/` exist for **drift detection**, not as data carriers. Each field's declaration encodes a different drift signal:

| Declaration                  | Key required in JSON? | Value can be `null`? | Use for                                                        |
|------------------------------|-----------------------|----------------------|----------------------------------------------------------------|
| `field: Type`                | yes                   | no                   | Fixture-always-present with non-null value (the common case).  |
| `field: Type?` *(no default)*| **yes**               | yes                  | Key always present in fixture, value sometimes `null`.         |
| `field: Type? = null`        | no                    | yes                  | Spec-only / forward-compat — key may be absent entirely.       |

Rationale: a missing default makes the field required by deserialization (absence triggers `MissingFieldException`). The type's `?` is independent — it controls whether `null` is a valid *value*. Combining the two encodes "the API always sends this key, sometimes with a null value" as a distinct signal from "the API may or may not send this key at all".

### Fixture rules + spec-forward-compat

When deciding nullability:

- **Required** (Tier 1) only when the captured fixture has the field on every record with a non-null value. Fixture is ground truth.
- **Required key, nullable value** (Tier 2) when the fixture always has the key but its value is sometimes `null`.
- **Optional / spec-only** (Tier 3, `Type? = null`) for fields the Buxfer API spec mentions but no captured fixture carries. Keeps the schema forward-compatible (the validator accepts the new field gracefully) and serves as documentation of what the spec promises.

If the API doc and fixture disagree, the fixture wins for required-fields decisions; spec-only divergent fields go in as Tier 3.

### Model-as-schema validation

`BuxferClient` returns raw `JsonElement` (`JsonArray` for list endpoints, `JsonObject` for the wrapper-shaped `/transactions` and the write endpoints). The data path forwards the raw response to Claude unchanged.

Validation runs as a **side effect** via the `validateSchema<T>` helper in `BuxferClient`:

- Strict decode against the schema using a dedicated `validatorJson` (`ignoreUnknownKeys = false`) — strict so unknown fields surface as drift.
- `SerializationException` → `log.warn("Schema drift on ...")`. Never thrown.
- Any other exception → `log.error("Schema validation failed unexpectedly ...")`. Also never thrown.
- The data path uses the production `buxferJson` (permissive `ignoreUnknownKeys = true`) and is never affected by validation outcomes.

The shared `getValidatedList<Schema>(path)` helper handles the common shape (HTTP GET → envelope strip → array extract → validate → return). Endpoints with non-standard shapes (currently just `getTransactions` with its `transactions[]` + `numTransactions` wrapper, and the write endpoints with their `JsonObject` responses) inline the same pattern: HTTP, envelope strip, `validateSchema<Schema>(body, path)`, return body.

### Other invariants worth preserving

- **No stdout output anywhere.** MCP communicates over stdio; any `print`/`println`/`System.out` breaks the protocol. Logback config is file-only by default; fatal startup errors go to stderr.
- **`@Volatile` for the auth token**, set once on login and read many times. No other shared mutable state in `BuxferClient`.
- **`AutoCloseable` resources use `.use { ... }`** in tests; the long-running server closes via JVM shutdown hook in `Main.kt`.
- **No star imports.** No multi-class files.
- **Run gradle via ASDF shims**: `PATH="$HOME/.asdf/shims:$PATH" gradle test` from `kotlin/`.

---

## Remaining follow-ups

All three are **wait-for-signal** — no operational evidence yet that any is needed. Nothing in flight.

### 1. Resilience policy decisions

`BuxferClient` already wraps every Ktor / IO failure as `BuxferApiException` so the tool layer's contract stays clean. The deferred sub-decisions:

- **Retry on transient failures** (5xx, IOException, HttpRequestTimeoutException). The LLM can re-invoke a failed tool itself; speculative retry would mask real Buxfer behavior the operator should see. Revisit if production flakiness becomes a real pain point.
- **Re-login on 401 / expired token.** Buxfer's docs are silent on token-expiration semantics and the error shape it returns for invalid vs. expired tokens (verified against [www.buxfer.com/help/api](https://www.buxfer.com/help/api) and `shared/api-spec/buxfer-api.md`). Heuristic detection risks an infinite re-login loop on a wrong-credential error. Wait for empirical evidence before adding policy. Options if it becomes painful: (a) expose a `buxfer_relogin` MCP tool; (b) detect a specific error message string and re-login transparently — both depend on Buxfer's actual error shape.
- **Per-call timeout overrides for slow endpoints (statement upload).** Only relevant if `uploadStatement` actually trips the 30s default in practice; no reports yet.

### 2. Group fixture gap

`shared/test-fixtures/wiremock/__files/groups.json` is empty (`[]`). The captured account had no groups, so we have no fixture evidence to tighten any field. `Group` and `Member` schemas are fully Tier 3 (every field nullable with `null` default); the validator effectively no-ops on Group responses. Tighten when a real fixture is captured (run `api-recordings/run-capture.sh` against an account that has at least one group).

The same caveat applies to `upload_statement.json`, which is `{}` (the endpoint requires manual capture per `api-recordings/CLAUDE.md`). The test stack works around it via `MockEngineSupport.UPLOAD_STATEMENT_OK_BODY`.

### 3. Multi-language polish (low priority)

The TypeScript and Python implementations haven't been started. Out of the Kotlin module's scope, but the cross-language contract (`shared/api-spec/buxfer-api.md` + fixtures) is in good shape for whoever picks them up. The schema lessons learned here (three-tier nullability, fixture rules, spec-only-nullable, model-as-schema validation) would translate.

---

## Resuming notes

If a future session needs context beyond this file:

- **Per-endpoint migration history**: see git log for commits dated 2026-05-07 around `kotlin/src/main/kotlin/com/buxfer/mcp/api/models/`. Each endpoint migrated as its own commit with a rollout note in this file at the time.
- **Test architecture**: see `kotlin/CLAUDE.md` §Testing and the existing `*Test.kt` / `*IntegrationTest.kt` pairs.
- **MCP SDK gotchas**: see `kotlin/CLAUDE.md` §"SDK gotchas" — the `ServerCapabilities()` `tools = null` trap and the `kotlin-sdk-testing` `ChannelTransport.createLinkedPair()` for in-process server↔client wiring.
- **API doc / fixture / model cross-reference research**: was captured in this file under per-endpoint rollout notes during the model-layer migration; pruned now. Re-run by reading `shared/api-spec/buxfer-api.md`, the relevant fixture in `shared/test-fixtures/wiremock/__files/`, and the model class side-by-side.
