# Phase 5 — Test Review (findings, deferred to next session)

## Context

Phase 5 reviewed every production layer (Models → API client → Tools → Server → Main). This document is the bottom-up review of the corresponding tests, captured in one pass while the production-side decisions were still in working memory. Address in a follow-up session.

**Production conventions to preserve while addressing these:**
- Identity field (`id`, or `entity` for Loan) is the only required model field; everything else is nullable or `emptyList()` default.
- `Json { ignoreUnknownKeys = true; encodeDefaults = false }` — drift-tolerant decode, compact encode.
- `BuxferApiException` is the unified error type (catches `SerializationException` via `traced`, surfaces method+path).
- `BuxferClient` is `AutoCloseable`; loggers in companion objects.
- `mcpTool` helper wraps log+encode+isError pattern for the simple list tools.
- `JsonObjectExtensions.kt` exposes `requireInt/Double/String` (throw on missing) and `optString` (null on missing).

---

## 🔴 Issues worth fixing

### 1. `TransactionToolsTest` doesn't cover the new `require*` arg validation

[`TransactionTools`](src/main/kotlin/com/buxfer/mcp/tools/TransactionTools.kt) was refactored to throw `IllegalArgumentException("Missing or malformed required argument 'X'")` on missing args (deleteTransaction, editTransaction, addTransaction, uploadStatement). [`TransactionToolsTest`](src/test/kotlin/com/buxfer/mcp/tools/TransactionToolsTest.kt) tests happy paths and the client-throws path, but not the missing-args path.

**Add tests:**
- `deleteTransaction with no id surfaces isError`
- `editTransaction with missing accountId surfaces isError`
- `addTransaction with missing description surfaces isError`
- Each asserts `result.isError == true` AND `text` contains the missing arg name

This covers the safety net we built into the tool layer.

### 2. No direct tests for `JsonObjectExtensions.kt`

The four extensions (`requireInt`, `requireDouble`, `requireString`, `optString`) are exercised only transitively through `TransactionToolsTest`. The exact error messages, the missing-vs-malformed distinction (e.g., string where int expected), and `null` argument handling aren't asserted directly.

**Add `JsonObjectExtensionsTest.kt`:**
- `requireInt throws when arg is missing`
- `requireInt throws when arg is wrong type` (e.g., `"abc"`)
- `requireInt returns the int when present`
- Same shape for `requireDouble`, `requireString`
- `optString returns null when missing` and `optString returns the value when present`
- Confirm the exact error message format (it's a contract — TransactionTools relies on it for the user-visible text in `isError` results)

### 3. No direct tests for the `mcpTool` helper

Same pattern as JsonObjectExtensions — exercised via the 7 simple `*ToolsTest` classes but never directly.

**Add `McpToolTest.kt`:**
- `mcpTool returns CallToolResult wrapping encoded value`
- `mcpTool surfaces isError on exception with message included`
- `mcpTool calls log.info on entry and log.error on failure`
- Optionally: assert that the encoded JSON respects `encodeDefaults = false` (no null fields)

### 4. `BuxferMcpServerIntegrationTest` setup boilerplate repeated 5×

```kotlin
val (clientTransport, serverTransport) = ChannelTransport.createLinkedPair()
launch { BuxferMcpServer(httpClient).start(serverTransport) }
val client = mcpClient(Implementation("integration-test", "0.0.0"), transport = clientTransport)
// ... test body ...
client.close()
```

Each test repeats this. Extract a helper:

```kotlin
private suspend fun TestScope.launchMcpClient(): Client {
    val (clientTransport, serverTransport) = ChannelTransport.createLinkedPair()
    launch { BuxferMcpServer(httpClient).start(serverTransport) }
    return mcpClient(Implementation("integration-test", "0.0.0"), transport = clientTransport)
}
```

Each test becomes a 1-line setup + the assertion, mirroring the dedup work we did in production (`mcpTool`, `getList`).

### 5. Tool model fixtures construct verbose full-field instances

Pre-Phase-5, models required all fields (`Transaction(id, description, amount, accountId, accountName, ...)` with 14+ args). Post-Phase-5, only identity is required:

```kotlin
// Was (still works, but verbose):
Transaction(id = 33040, description = "...", amount = 0.01, accountId = 10350,
    accountName = "Test Account", date = "2026-04-26", tags = "", type = "expense",
    status = "cleared", transactionType = "expense", expenseAmount = 0.01,
    tagNames = emptyList(), isFutureDated = false, isPending = false)

// Could be (minimum reproducer for the assertion):
Transaction(id = 33040, type = "expense", transactionType = "expense", date = "2026-04-26")
```

[`TransactionToolsTest`](src/test/kotlin/com/buxfer/mcp/tools/TransactionToolsTest.kt) lines 28-65 (5 fixture transactions × 14 fields) and [`ReminderToolsTest`](src/test/kotlin/com/buxfer/mcp/tools/ReminderToolsTest.kt) lines 30-37 are the most affected. Trim to only the fields the test actually asserts on. **Note:** this becomes moot if the [model-layer challenge](IMPROVEMENT_PLAN.md) lands and we drop response models entirely.

---

## 🟡 Worth doing but not urgent

### 6. The `getTransactions returns parsed Transaction list` test is heavy

[`BuxferClientTest`](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) lines 87-108 assert on 7 fields of one transaction plus 3 fields of another. With the fixture stable, this is a real contract test — but if any field changes, the failure message points at one assertion in a 12-line block. Splitting into "deserializes core scalars" + "deserializes nested transfer accounts" makes diagnostics sharper.

### 7. Budget test asserts on 16 fields in a single satisfies block

[`BuxferClientTest`](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) lines 162-178. Same concern as #6 but more extreme. Defensible (it's a single round-trip; one assertion block reflects that), just hard to skim.

### 8. `MockEngine` setup has 13 inline `path.endsWith(...)` branches

[`BuxferClientTest`](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) lines 26-42. Adding a new endpoint requires a new line. Could be a data-driven map, similar in spirit to the `getList` production helper:

```kotlin
private val defaultFixtures = mapOf(
    "/login" to "login", "/accounts" to "accounts", /* ... */
)
private fun mockEngine(overrides: Map<String, String> = emptyMap()) = MockEngine { request ->
    val body = overrides[/* path-key */] ?: defaultFixtures[/* path-key */]?.let(TestFixtureLoader::load) ?: errorBody
    respond(body, ...)
}
```

Marginal payoff.

### 9. Duplicated upload_statement workaround

The empty `__files/upload_statement.json` fixture is worked around inline in two places:
- [`BuxferClientTest`](src/test/kotlin/com/buxfer/mcp/api/BuxferClientTest.kt) line 34: hardcoded JSON in the MockEngine fallthrough
- [`BuxferClientIntegrationTest`](src/test/kotlin/com/buxfer/mcp/api/BuxferClientIntegrationTest.kt) lines 119-124: inline WireMock stub

Could extract a single constant `UPLOAD_STATEMENT_OK_BODY` shared between both. Better still: capture a real upload_statement fixture (the actual remediation).

### 10. The 7 simple `*ToolsTest` classes have identical "client throws" tests

Same pattern repeated 7×:
```kotlin
@Test
fun `tool returns error result when client throws`() = runTest {
    coEvery { mockClient.getX() } throws BuxferApiException("boom")
    val result = tools.listX()
    assertThat(result.isError).isTrue()
    assertThat((result.content[0] as TextContent).text).contains("Error: boom")
}
```

Could extract a parameterized test on the `mcpTool` helper itself (see #3) so the tool classes don't each retest the same wrapper.

### 11. `BuxferClientConfig` defaults / env-var resolution untested

Construct with no env → defaults. Construct with `BUXFER_API_BASE_URL` set → override. Construct with timeouts overridden → values flow through. None of this is covered. With the resilience review pending (which may add more env-driven knobs), this becomes more important.

### 12. `BuxferMcpServerIntegrationTest` block comments could be a class-level table

Each scenario explains itself in a 2-3 line comment. Useful, but scattered. A short table at the class top mapping `scenario → layer it exercises` would orient readers faster:

| Scenario | Exercises |
|---|---|
| listTools | Initialize handshake + tools/list path |
| list_accounts round-trip | Full pipeline, deserialization → re-encode |
| filter threading | Args → query params plumbing via WireMock journal |
| add_transaction form post | POST/form path |
| error envelope | BuxferApiException → isError surface |

### 13. Inconsistent test-name style in `TransactionToolsTest`

Mixes "returns JSON of...", "passes id and fields to client", "returns confirmation on success". Pick one (probably "verb + observable result").

### 14. `launch { ... }` server jobs in integration tests are never explicitly cancelled

`runTest` cleans up the scope, but each test could `serverJob.cancel()` for explicitness. Possibly moot if the helper extraction (#4) handles it.

---

## 🟢 Considered and accepted as-is

- **Models have no separate tests.** Coverage flows through `BuxferClientTest` deserialization assertions. By design — models are pure data.
- **`BuxferMcpServerTest` is intentionally minimal** (2 tests via `toolDescriptors`). Phase 4a's scope was deliberate.
- **`PER_CLASS` lifecycle** for WireMock in both integration suites — correct (startup is expensive).
- **Hardcoded "12 tools" assertion** in `BuxferMcpServerTest` and `BuxferMcpServerIntegrationTest` — tests should be explicit; the maintenance burden of updating in two places when tool count changes is the right trade.
- **`TestFixtureLoader`** (17 lines, lazy init, clear errors) and **`WireMockSupport`** (post-restructure) are clean.
- **`parse error surfaces field, type, and endpoint context`** is the strongest test in `BuxferClientTest` — validates the whole error-pipeline contract.
- **Filter-threading test** uses WireMock's request journal to assert the wire format. Exemplary.
- **`AutoCloseable` cleanup** consistently applied (per-class teardown for the long-lived field, `.use { }` for ad-hoc instances).

---

## Recommended order for next session

1. **#1, #2, #3** together — covers the safety nets we just built in production. Highest value: each adds tests for code paths that currently rely on transitive coverage. Probably ~5-10 new test cases total. One commit.
2. **#4** — integration test setup helper. Pure refactor, no behavior change. One commit.
3. **#5** — fixture trimming, IF the model-layer challenge (in IMPROVEMENT_PLAN.md) hasn't already obviated the question. One commit.
4. The 🟡 list — pick what feels worth it; none are blocking.

After this, Phase 5 is genuinely closed.
