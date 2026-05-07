# Kotlin Module — Multi-Session Improvement Plan

A staged plan to harden the Kotlin implementation. Each phase is self-contained
and can run in its own session. Mark a phase complete by checking it off here.

---

## Phase 1 — AssertJ migration

**Goal:** Replace JUnit `assertEquals` with AssertJ for fluent, readable
assertions, including JSON tree assertions.

### Confirmed decisions

- **JSON-assertion library:** `net.javacrumbs.json-unit:json-unit-assertj`
  — replaces the current `kotlinx.serialization` tree walks. Picked
  specifically for the better failure messages it produces (full path to
  the diff, expected-vs-actual JSON pretty-printed) versus hand-walked
  trees that fail with "expected 42 but was 13" with no context.

### Steps

1. Add to `kotlin/build.gradle.kts` test dependencies:
   - `org.assertj:assertj-core:3.27.x`
   - `net.javacrumbs.json-unit:json-unit-assertj:3.x`
2. Migrate test files in this order (smallest first — keeps each commit tight):
   - [src/test/kotlin/com/buxfer/mcp/tools/AccountToolsTest.kt](src/test/kotlin/com/buxfer/mcp/tools/AccountToolsTest.kt) (62 lines)
   - [src/test/kotlin/com/buxfer/mcp/tools/LookupToolsTest.kt](src/test/kotlin/com/buxfer/mcp/tools/LookupToolsTest.kt) (139 lines)
   - [src/test/kotlin/com/buxfer/mcp/tools/TransactionToolsTest.kt](src/test/kotlin/com/buxfer/mcp/tools/TransactionToolsTest.kt) (160 lines)
   - [src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) (173 lines)
3. Replace patterns:
   - `assertEquals(a, b)` → `assertThat(b).isEqualTo(a)`
   - `assertTrue(x)` → `assertThat(x).isTrue()`
   - JSON walks like `parsed[0].jsonObject["id"]!!.jsonPrimitive.int == 42` →
     `assertThatJson(text).inPath("$[0].id").isEqualTo(42)`
4. Run `gradle test` after each file; ensure all green before moving on.

### Verification

- All existing tests still pass (no behavior change).
- No remaining import of `org.junit.jupiter.api.Assertions.*`.

---

## Phase 2 — Logging via SLF4J + Logback rolling file

**Goal:** Replace `slf4j-nop` with real logging that writes to a rolling file.
Stdout MUST remain silent (it is the MCP transport).

### Confirmed decisions

- **Log file location:** `./logs/server.log` — relative to the server's
  working directory. The server **never** writes outside its own working
  directory (portability constraint: the deployment must be self-contained
  and require no access to `${HOME}` or any other path). Operators who need
  a different path can set `BUXFER_LOG_DIR` (must still be a path the
  process can write to without elevated permissions).
- **Rolling policy:** size+time-based — 10 MB per file, 14-day retention,
  total cap 100 MB.
- **Default level:** `INFO`, with `BUXFER_LOG_LEVEL` env override.

### Steps

1. Update `kotlin/build.gradle.kts`:
   - Remove `org.slf4j:slf4j-nop`
   - Add `ch.qos.logback:logback-classic:1.5.x`
2. Create `kotlin/src/main/resources/logback.xml`:
   - Single `RollingFileAppender` writing to the path above
   - **No `ConsoleAppender`** — explicit comment explaining MCP stdio constraint
   - Resolve log dir at runtime via `${BUXFER_LOG_DIR:-./logs}`
3. Add `private val log = LoggerFactory.getLogger(this::class.java)` to:
   - [src/main/kotlin/com/buxfer/mcp/Main.kt](src/main/kotlin/com/buxfer/mcp/Main.kt)
   - [src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt](src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt)
   - [src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt](src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt)
   - All three `tools/*Tools.kt` files
4. Instrument:
   - `Main`: log startup banner, configured log dir, login result. Keep
     existing `System.err.println` for fatal startup errors so the operator
     who launched the process sees them.
   - `BuxferClient`: DEBUG for each HTTP request (method + path, NEVER token
     or credentials), DEBUG for response status, ERROR for thrown
     `BuxferApiException`.
   - `BuxferMcpServer` / tools: INFO for each tool invocation with arg
     summary (redacted), ERROR for caught exceptions.

### Verification

- `gradle run` produces no stdout output, but a `./logs/server.log` file
  appears under the directory `gradle run` was launched from (and nowhere
  else on the filesystem — confirm by running from a fresh tmp dir).
- Send an MCP `tools/list` request via stdio and confirm the response is
  clean JSON-RPC (no log lines mixed in).
- Force a login failure; the error appears in the log file AND on stderr.
- Rolling: smoke-test with a tiny `maxFileSize` to confirm rollover works.

---

## Phase 3 — Configurable Buxfer API base URL

**Goal:** Make the Buxfer API endpoint injectable on `BuxferClient` so tests
(Phase 4) and advanced operators can point the client at a different host
(e.g. WireMock, a staging environment). This phase is a **prerequisite for
Phase 4**.

### Steps

1. Update [src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt](src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt):
   - Replace `companion object { private const val BASE_URL = "..." }` with
     a `companion object { const val DEFAULT_BASE_URL = "..." }` plus a
     constructor parameter `baseUrl: String = DEFAULT_BASE_URL`
   - Use `baseUrl` (not `BASE_URL`) in every request builder
2. Update [src/main/kotlin/com/buxfer/mcp/Main.kt](src/main/kotlin/com/buxfer/mcp/Main.kt):
   - Read `BUXFER_API_BASE_URL` env var; fall back to default if unset
   - Pass it to the `BuxferClient(...)` constructor
3. Update existing tests:
   - [src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) — no
     behavior change (default keeps current URL); just confirm tests still
     green
   - Add one new test: passing a custom `baseUrl` makes requests hit that
     host (verify with `MockEngine` request inspection)

### Verification

- `gradle test` green
- `BUXFER_API_BASE_URL=https://example.test gradle run` actually calls
  `example.test` (verify with a quick netcat or by watching the failure
  message)

---

## Phase 4 — Tests for missing pieces (server + integration)

**Goal:** Cover the currently untested integration points and add an
end-to-end test that exercises the full MCP server against a fake Buxfer API.

### Sub-phase 4a — Unit tests for server bootstrap

Test target: [src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt](src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt)

Add `BuxferMcpServerTest`:
- Verify all 12 tools are registered with the expected names.
- Verify each tool description is non-empty.
- May require exposing the registered-tool list via a test-only accessor or
  using the SDK's `ListToolsRequest` if available.

### Sub-phase 4b — Integration test with WireMock + in-memory transport

**Setup prerequisites — document explicitly in `kotlin/CLAUDE.md`:**
- The integration test runs WireMock embedded in the JVM (no Docker
  required for `gradle test`). The Docker `compose.yml` in
  `api-recordings/` is only for fixture capture, NOT for running tests.
- The test loads stub mappings from `../shared/test-fixtures/wiremock/mappings/`.
  These files are produced by the `api-recordings/` capture workflow. If
  the directory is empty or missing, the integration test will skip (or
  fail with a clear "run capture first" message — pick one in 4b
  implementation).
- Operators reproducing failures locally need to either commit the
  anonymized fixture files OR run `./run-capture.sh` from `api-recordings/`
  with valid Buxfer credentials. Document both paths.

**Tooling:**
- Add test dependency `org.wiremock:wiremock:3.x` (embedded JVM library,
  NOT the Docker container — runs in the test process).
- Reuse the existing WireMock mappings at `../shared/test-fixtures/wiremock/mappings/`
  (already produced by the `api-recordings/` module).
- Configure WireMock to bind to a random free port; pass that URL to
  `BuxferClient` via the new `baseUrl` constructor parameter from Phase 3.

**Transport:**
- Use kotlin-sdk's `InMemoryTransport.createPair()` if exposed publicly; if
  not, copy the SDK's test-source `MockTransport` into `src/test/kotlin/`.
  Decision point — confirm what's available before starting.

**Test scenarios** (one per fixture endpoint plus a handful of compositions):
- Server initializes, lists 12 tools.
- `buxfer_list_accounts` round-trip returns the WireMock-served accounts JSON.
- `buxfer_list_transactions` with filters builds the right query string and
  parses results.
- `buxfer_add_transaction` posts form data and returns the created txn.
- One error-path scenario: WireMock returns `{ "response": { "status":
  "error", "error": "..." } }` and the tool surfaces `isError = true`.

This is a "showcase" test — comments should walk through the round-trip so a
reader sees the whole pipeline working end-to-end.

### Verification

- `gradle test` runs unit + integration tests in one go (single test task).
- Coverage report (optional, via JaCoCo) shows `BuxferMcpServer` + `Main`
  exercised.

---

## Phase 5 — Bottom-up code review walkthrough

**Format:** interactive review session(s) with the user. The plan here is
just the order of files and the lens to apply at each step.

### Review order (bottom-up by dependency direction)

1. **Models** — [src/main/kotlin/com/buxfer/mcp/api/models/](src/main/kotlin/com/buxfer/mcp/api/models/)
   `Account.kt`, `Transaction.kt`, `Tag.kt`, `Budget.kt`, `Reminder.kt`,
   `Group.kt`, `Contact.kt`, `Loan.kt`
2. **API client** — [src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt](src/main/kotlin/com/buxfer/mcp/api/BuxferClient.kt),
   [src/main/kotlin/com/buxfer/mcp/api/BuxferApiException.kt](src/main/kotlin/com/buxfer/mcp/api/BuxferApiException.kt)
3. **Tools** — [src/main/kotlin/com/buxfer/mcp/tools/](src/main/kotlin/com/buxfer/mcp/tools/)
   `LookupTools.kt`, `AccountTools.kt`, `TransactionTools.kt`
4. **Server** — [src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt](src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt)
5. **Entry point** — [src/main/kotlin/com/buxfer/mcp/Main.kt](src/main/kotlin/com/buxfer/mcp/Main.kt)
6. **Tests** — corresponding test file alongside each main file

### Lens for each file

- Naming & idioms — does it read like idiomatic Kotlin?
- Error handling — every failure mode has a clear path; no silent catches.
- Nullability — `?` and `!!` justified; defaults sensible.
- Coroutines — suspending where it should; no blocking on the IO thread.
- Test coverage — every branch hit by a test in the matching `*Test.kt`.

### Output

Each pass produces either: (a) a short list of accepted changes, applied
immediately, or (b) follow-up items captured at the bottom of this doc.

---

## Cross-phase notes

- Run `gradle test` and `gradle compileKotlin` from `kotlin/` with
  `PATH="$HOME/.asdf/shims:$PATH"` (per repo convention).
- Each phase ends with all tests green and a single focused commit (or
  sequence of small commits — user preference).
- If a session runs out of context, leave a "Resuming Phase N" note here
  with the file currently in flight.

---

## Resuming review (2026-05-05)

**Done:** Phases 1–4 (1, 2, 3, 4a, 4b). 58 tests green.

Phase 4a notes (kept for context):
- `idea` plugin + `isDownloadSources = true` in [build.gradle.kts](build.gradle.kts) so the LSP indexes SDK sources directly.
- Two non-obvious SDK gotchas captured in [CLAUDE.md](CLAUDE.md) §"SDK gotchas" (`ServerCapabilities()` `tools = null` trap; no `InMemoryTransport` ships in 0.11.x — though see 4b for the published alternative).
- Public `toolDescriptors: Map<String, String?>` introspection accessor on `BuxferMcpServer` (kept `server` private; no `internal` backdoors).
- [BuxferMcpServerTest](src/test/kotlin/com/buxfer/mcp/BuxferMcpServerTest.kt) — 2 cases.
- Production bug found: bootstrap was passing `ServerCapabilities()` with `tools = null`; fixed to `ServerCapabilities(tools = ServerCapabilities.Tools())`. Server had been crashing on every startup; uncovered on first construction by the new test.

Phase 4b notes:
- Discovered `kotlin-sdk-testing:0.11.1` ships the supported `ChannelTransport.createLinkedPair()` for in-process server↔client wiring. Used that — no hand-rolled transport.
- Eliminated `internal var token` on `BuxferClient` (now `private`); existing [BuxferClientTest](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) goes through real `login(...)` against the MockEngine `/login` stub. The "login stores token" assertion rewrote as a behavioral check (next call carries the token).
- Added `start(transport: Transport)` overload to [BuxferMcpServer](src/main/kotlin/com/buxfer/mcp/BuxferMcpServer.kt) so tests can drive a non-stdio transport without exposing `server`.
- [WireMockSupport](src/test/kotlin/com/buxfer/mcp/testing/WireMockSupport.kt) — embedded WireMock helper, mirrors the Docker layout from [api-recordings/compose.yml](../api-recordings/compose.yml). Stages a tmp dir whose `__files` symlinks to the responses dir (Docker mounts `responses/` as `__files` directly; embedded WireMock needs the canonical `<root>/__files/<bodyFileName>` shape).
- [BuxferClientIntegrationTest](src/test/kotlin/com/buxfer/mcp/api/BuxferClientIntegrationTest.kt) — 14 scenarios, one per `BuxferClient` public method, real HTTP through embedded WireMock.
- [BuxferMcpServerIntegrationTest](src/test/kotlin/com/buxfer/mcp/BuxferMcpServerIntegrationTest.kt) — 5 scenarios exercising the full pipeline (Client ↔ ChannelTransport ↔ Server ↔ tools ↔ BuxferClient ↔ HTTP ↔ WireMock): listTools, list_accounts round-trip, filter threading via WireMock request journal, add_transaction form post, error envelope → `isError`.
- **Fixture bug found and fixed.** All 13 mappings in `shared/test-fixtures/wiremock/mappings/*.json` and the `api-recordings/generate-wiremock.sh` generator used `"urlPathEqualTo"` (a Java DSL method name) where WireMock 3.x JSON expects `"urlPath"`. Changed to `urlPath` everywhere; the Docker WireMock mappings work correctly now too.
- Known fixture gap (out of scope): `shared/test-fixtures/wiremock/__files/upload_statement.json` is `{}` (the endpoint requires manual capture per [api-recordings/CLAUDE.md](../api-recordings/CLAUDE.md)). The integration test for `uploadStatement` overrides the WireMock stub inline rather than relying on the empty file.

**Next:** Phase 5 — bottom-up code review walkthrough. Order and lens already specified above.

---

## Resuming review (2026-05-07)

**Done:** Phase 5 production-side walkthrough + the post-review test-suite follow-through. **89 tests green.**

Phase 5 production notes:
- Models slimmed to "identity-only required, everything else nullable / `emptyList()` default" — drift-tolerant decode via `Json { ignoreUnknownKeys = true; encodeDefaults = false }`.
- `BuxferApiException` unified as the single error type; `BuxferClient.traced` wraps `SerializationException` with method+path context.
- `mcpTool` helper extracted in [src/main/kotlin/com/buxfer/mcp/tools/McpTool.kt](src/main/kotlin/com/buxfer/mcp/tools/McpTool.kt) — wraps log + encode + isError for the simple list tools.
- `JsonObjectExtensions.kt` exposes `requireInt`/`requireDouble`/`requireString` (throw on missing) and `optString` (null on missing) — replaces silent-zero antipattern in `TransactionTools`.

Test-suite follow-through (closed as part of this resume):
- New direct-coverage suites: `JsonObjectExtensionsTest` (15 cases pinning the contract message), `McpToolTest` (5 cases), `BuxferClientConfigTest` (4 cases), `BuxferClientErrorHandlingTest` (3 tests extracted from `BuxferClientTest` to keep the latter under ~250 lines).
- Integration-test setup helper extracted (`launchMcpClient()` in [BuxferMcpServerIntegrationTest](src/test/kotlin/com/buxfer/mcp/BuxferMcpServerIntegrationTest.kt)).
- Shared `MockEngineSupport` utility ([src/test/kotlin/com/buxfer/mcp/testing/MockEngineSupport.kt](src/test/kotlin/com/buxfer/mcp/testing/MockEngineSupport.kt)) — data-driven endpoint→fixture map, plus `UPLOAD_STATEMENT_OK_BODY` constant shared between MockEngine and WireMock test tiers.
- Two items considered and deliberately NOT applied: deduping per-tool `client throws` tests (kept the per-tool pin) and trimming verbose model fixtures (kept realistic data).

**Next:** the two follow-ups below — resilience and the model-layer challenge.

---

## Follow-ups identified during Phase 5 review

These were flagged during the Phase 5 walkthrough but deferred to keep each commit focused. Address before the kotlin module is considered complete.

### Resilience to intermittent server failures

`BuxferClient` currently has no retry, no token refresh, no recovery story for transient failures. A flaky Buxfer connection means the MCP tool surfaces a one-shot error that Claude has no good way to recover from.

Original sub-decisions and outcome (2026-05-07):

- **Wrap `IOException` / `HttpRequestTimeoutException` / other Ktor types as `BuxferApiException`** — **Done.** `traced` now catches `HttpRequestTimeoutException` (extends `kotlinx.io.IOException`, needs its own arm), `java.io.IOException` (covers `ConnectException` / `UnknownHostException` / `SocketTimeoutException` / `SSLException`), `CancellationException` (rethrown without wrapping), and a final `Exception` arm for `FailToConnectException` and other engine-level failures. Tool layer's contract — every error from `BuxferClient` is `BuxferApiException` — is now intact. 3 tests in `BuxferClientErrorHandlingTest`. **92 tests green.**
- **Retry idempotent GETs on transient failures.** **Deferred.** No operational evidence yet that the network is flaky enough to warrant retry. The LLM can re-invoke a failed tool itself; speculative retry would mask real Buxfer behavior the operator should see. Revisit if real-world usage shows it's needed.
- **Re-login if a request returns 401 (expired token).** **Deferred.** Buxfer's docs are silent on token-expiration semantics and the error shape it returns for invalid vs. expired tokens (verified against both [www.buxfer.com/help/api](https://www.buxfer.com/help/api) and [shared/api-spec/buxfer-api.md](../shared/api-spec/buxfer-api.md)). Heuristic detection risks an infinite re-login loop on a wrong-credential error. Wait for empirical evidence before adding policy. If session expiration becomes a real pain point, options are (a) expose a `buxfer_relogin` MCP tool, or (b) detect a specific error message string and re-login transparently — both depend on Buxfer's actual error shape.
- **Per-call timeout overrides for slow endpoints (statement upload).** **Deferred.** Only relevant if `uploadStatement` actually trips the 30s default in practice; no reports yet.

### Challenge the fixed-model-class approach

This is an MCP server: Buxfer JSON in, JSON out to Claude. **Claude is the data consumer; it parses JSON itself.** The Phase 1 inventory confirmed that no response-model field is read programmatically in our code — every tool just `Json.encodeToString(model)` and forwards. Yet we maintain 14 model classes that hand-mirror the wire format, plus tests that exercise them.

#### Approach (decided 2026-05-07)

Drop response classes as **data carriers** but keep them as **drift-detection schemas**:
- `BuxferClient` returns the raw `JsonElement` (typically `JsonArray` or `JsonObject`) extracted from Buxfer's `response.<key>`. Tool layer forwards via `mcpTool` — `buxferJson.encodeToString(jsonElement)` produces the same wire shape Claude saw before.
- A side-effect `validateSchema<T>(json, path)` attempts a strict `decodeFromJsonElement<T>` using a dedicated `validatorJson` (`ignoreUnknownKeys = false`). On `SerializationException` it logs a WARNING — never throws. Production data flow is unaffected; the operator gets a precise drift signal (missing field, wrong type, unexpected key).
- Schema fields use a **three-tier nullability convention** that maps cleanly to kotlinx.serialization's deserialization rules. Each tier corresponds to a different drift signal:

  | Declaration                  | Key required in JSON? | Value can be `null`? | Use for                                                        |
  |------------------------------|-----------------------|----------------------|----------------------------------------------------------------|
  | `field: Type`                | yes                   | no                   | Fixture-always-present with non-null value (the common case).  |
  | `field: Type?` *(no default)*| **yes**               | yes                  | Key always present in fixture, value sometimes `null`.         |
  | `field: Type? = null`        | no                    | yes                  | Spec-only / forward-compat — key may be absent entirely.       |

  Rationale: a missing default makes the field required by deserialization (absence triggers `MissingFieldException`). The type's `?` is independent — it controls whether `null` is a valid *value*. Combining the two lets us encode "the API always sends this key, sometimes with a null value" as a distinct signal from "the API may or may not send this key at all" — both of which today's fixtures+spec evidence supports for different fields.

- Spec-only fields (documented in the Buxfer API spec but absent from every captured fixture) land in the third tier (`Type? = null`). The validator (`ignoreUnknownKeys = false`) won't fire warnings on their absence, but if Buxfer ever starts sending them, the schema accepts the new fields gracefully — keeping the schema forward-compatible and serving as documentation of the spec contract.
- Request DTOs (`AddTransactionParams`, `TransactionFilters`) keep their typed shape — they have a real Kotlin-args-to-wire role.

#### Pilot outcome (2026-05-07) — Accounts

- `Account.kt` tightened: 5 fixture-always-present fields non-nullable (`id`, `name`, `bank`, `balance`, `currency`). Spec-only `lastSynced` added as nullable per the spec-forward-compat policy (initially skipped in the pilot, added retroactively when Budget revealed the same kind of spec/fixture divergence and the policy crystallised).
- `BuxferClient.getAccounts()` now returns `JsonArray`. `validateSchema<List<Account>>` runs as a side effect.
- `AccountTools.listAccounts` body unchanged (the existing `mcpTool` one-liner forwards `JsonArray` natively).
- Tests: `getAccounts returns parsed Account list` rewritten to `getAccounts returns JsonArray of account objects`; new `getAccounts logs schema-drift warning on missing required field` validates the Logback warning fires on a malformed fixture. **93 tests green.**
- Net: production deletes the typed-data round-trip for one endpoint; gains operator-facing drift warnings on the same endpoint.

#### Rollout queue

The `validateSchema` helper is in place; each remaining endpoint follows the same shape (schema tightening + return-type change + tests update). Per-type findings from the API-doc / fixture / Kotlin-model cross-reference live in the rollout notes:

- **Tag** — **Done (2026-05-07).** `id` / `name` / `relativeName` non-nullable; `parentId` stays nullable. `BuxferClient.getTags()` returns `JsonArray` and validates via `validateSchema<List<Tag>>`. Tightened `Tag` is also referenced by unmigrated `Budget.tag` and `Reminder.tags` — the strict-decode window for partial nested Tags closes as those endpoints migrate (next in queue). The parse-error test in `BuxferClientErrorHandlingTest` was repointed from `/tags` to `/budgets`.
- **Contact** — **Done (2026-05-07).** All 4 fields (`id`, `name`, `email`, `balance`) tightened to non-nullable. `BuxferClient.getContacts()` returns `JsonArray` and validates via `validateSchema<List<Contact>>`.
- **Loan** — **Done (2026-05-07).** All 4 fields (`entity`, `type`, `balance`, `description`) tightened to non-nullable. `BuxferClient.getLoans()` now uses the shared `getValidatedList<List<Loan>>("/loans")` helper.
- **Transaction** — biggest scope. Fixture rich; many fields not in spec (`transactionType`, `expenseAmount`, `tagNames`, `isFutureDated`, `isPending`). Conditional fields (`fromAccount`/`toAccount` only on transfers); `accountId` legitimately nullable on transfers. Mystery: `transactionType` vs `type` — possible duplicate, worth user discussion.
- **Budget** — **Done (2026-05-07).** 16 fixture-always-present fields tightened to non-nullable; `stopDate` stays nullable (always present in the fixture but always with value `null`); spec-only `remaining` / `currentPeriod` / `tags[]` / `keywords[]` added as nullable per the spec-forward-compat policy. `budgetId` and `id` are duplicates in the fixture and both kept required. `tag` is now non-nullable, leaning on the already-tightened `Tag` schema. `BuxferClient.getBudgets()` uses `getValidatedList<List<Budget>>("/budgets")`. Parse-error test repointed from `/budgets` to `/reminders`.
- **Reminder** — similar to Budget. Spec talks about `period`, fixture has `periodUnit` + `periodSize`. Mystery: `transactionType` (int) alongside `type` (string).
- **Group / Member** — fixture is empty (`[]`). Can't infer schema from fixture. Defer until a real fixture is captured, or document decisions from spec only.
- **AccountRef** (nested in Transaction transfer fields) — `id` always, `name` always when present.
- **TransactionsResult** wrapper — fixture has `"numTransactions": "5"` (string) but Kotlin currently declares `Int` and reads it via `.toIntOrNull()` in `BuxferClient`. Schema either matches the wire form (string) or stays Int and the validator gets a warning.
- **UploadStatementResult** wrapper — fixture is empty (`{}`); a capture gap. Schema can mirror spec: `uploaded: Int`, `balance: Double`.
