package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.PayerShare
import com.buxfer.mcp.api.models.TransactionFilters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TransactionTools

    // BuxferClient.getTransactions() now returns the raw JsonObject Buxfer's response carries
    // (envelope-stripped). Build the test fixture to mirror that shape: a `transactions` array
    // plus the wire-format `numTransactions` (a string, not an int — see TransactionsResult).
    private val fixtureTransactionsBody: JsonObject = buildJsonObject {
        putJsonArray("transactions") {
            addJsonObject {
                put("id", 33040); put("type", "expense"); put("transactionType", "expense")
                put("isFutureDated", false); put("isPending", false)
            }
            addJsonObject { put("id", 33026) }
            addJsonObject { put("id", 39962) }
            addJsonObject {
                put("id", 22654)
                putJsonObject("fromAccount") { put("id", 18027); put("name", "Test Account 18027") }
                putJsonObject("toAccount") { put("id", 60885); put("name", "Test Account 60885") }
            }
            addJsonObject { put("id", 22653) }
        }
        put("numTransactions", "5")
    }

    // Write endpoints (addTransaction / editTransaction / uploadStatement) now return raw
    // JsonObject too. Per-test inline fixtures (built via `buildJsonObject`) keep each
    // test's mock body close to its assertions.

    @BeforeEach
    fun setUp() {
        tools = TransactionTools(mockClient)
        // Default for tests that exercise the `"me"` marker resolution. Tests that
        // need the unauthenticated case override this with `every { ... } returns null`.
        every { mockClient.currentUserEmail } returns "me@example.com"
    }

    @Test
    fun `listTransactions returns JSON with transactions and count`() = runTest {
        coEvery { mockClient.getTransactions(any()) } returns fixtureTransactionsBody

        val result = tools.listTransactions(null)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.transactions").isArray.hasSize(5)
        // numTransactions is forwarded as the wire string (Buxfer quotes it).
        assertThatJson(text).inPath("$.numTransactions").isString().isEqualTo("5")
        assertThatJson(text).inPath("$.transactions[0].id").isEqualTo(33040)
        assertThatJson(text).inPath("$.transactions[0].type").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].transactionType").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].isFutureDated").isEqualTo(false)
        assertThatJson(text).inPath("$.transactions[0].isPending").isEqualTo(false)
        // transfer has fromAccount/toAccount
        assertThatJson(text).inPath("$.transactions[3].fromAccount.id").isEqualTo(18027)
        assertThatJson(text).inPath("$.transactions[3].toAccount.id").isEqualTo(60885)
    }

    @Test
    fun `listTransactions with filters passes them to client`() = runTest {
        val emptyBody = buildJsonObject {
            put("transactions", JsonArray(emptyList()))
            put("numTransactions", "0")
        }
        coEvery { mockClient.getTransactions(any()) } returns emptyBody
        val args: JsonObject = buildJsonObject {
            put("accountId", 10350)
            put("startDate", "2026-04-01")
            put("endDate", "2026-04-30")
        }

        tools.listTransactions(args)

        coVerify {
            mockClient.getTransactions(
                TransactionFilters(accountId = 10350, startDate = "2026-04-01", endDate = "2026-04-30")
            )
        }
    }

    @Test
    fun `addTransaction returns JSON of created transaction`() = runTest {
        val addedTransaction = buildJsonObject {
            put("id", 33645); put("description", "Test Transaction")
        }
        coEvery { mockClient.addTransaction(any()) } returns addedTransaction
        val args: JsonObject = buildJsonObject {
            put("description", "Test Transaction")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
            put("type", "expense")
        }

        val result = tools.addTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.description").isEqualTo("Test Transaction")
    }

    @Test
    fun `editTransaction returns JSON of updated transaction`() = runTest {
        val editedTransaction = buildJsonObject {
            put("id", 33645); put("description", "Test Transaction (edited)")
        }
        coEvery { mockClient.editTransaction(33645, any()) } returns editedTransaction
        val args: JsonObject = buildJsonObject {
            put("id", 33645)
            put("description", "Test Transaction (edited)")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
            put("type", "expense")
        }

        val result = tools.editTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.description").isEqualTo("Test Transaction (edited)")
    }

    @Test
    fun `deleteTransaction forwards Buxfer's status-OK response body`() = runTest {
        // Live API returns `{"status":"OK"}` after the standard envelope strip — see
        // the captured fixture `transaction_delete.json`. The tool forwards it
        // verbatim instead of synthesizing a richer (and dishonest) shape.
        val deleteBody = buildJsonObject { put("status", "OK") }
        coEvery { mockClient.deleteTransaction(33645) } returns deleteBody
        val args: JsonObject = buildJsonObject { put("id", 33645) }

        val result = tools.deleteTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.status").isEqualTo("OK")
    }

    @Test
    fun `deleteTransaction surfaces isError when client throws (non-OK status)`() = runTest {
        // BuxferClient.deleteTransaction throws BuxferApiException when the API
        // returns a non-OK status; the tool's runCatching converts that into
        // isError=true so the LLM sees the failure.
        coEvery { mockClient.deleteTransaction(33645) } throws
            BuxferApiException("non-OK status: failed")
        val args: JsonObject = buildJsonObject { put("id", 33645) }

        val result = tools.deleteTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("non-OK status")
    }

    @Test
    fun `uploadStatement returns uploaded count and balance`() = runTest {
        val uploadResult = buildJsonObject {
            put("uploaded", 15); put("balance", 1234.56)
        }
        coEvery { mockClient.uploadStatement(any(), any(), any()) } returns uploadResult
        val args: JsonObject = buildJsonObject {
            put("accountId", 10350)
            put("statement", "csv-content")
        }

        val result = tools.uploadStatement(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.uploaded").isEqualTo(15)
        assertThatJson(text).inPath("$.balance").isEqualTo(1234.56)
    }

    @Test
    fun `listTransactions surfaces isError when client throws`() = runTest {
        coEvery { mockClient.getTransactions(any()) } throws BuxferApiException("boom")

        val result = tools.listTransactions(null)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }

    @Test
    fun `deleteTransaction surfaces isError when id is missing`() = runTest {
        val result = tools.deleteTransaction(buildJsonObject { })

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'id'")
    }

    @Test
    fun `editTransaction surfaces isError when accountId is missing`() = runTest {
        val args: JsonObject = buildJsonObject {
            put("id", 33645)
            put("description", "Test Transaction (edited)")
            put("amount", 0.01)
            put("date", "2026-04-26")
        }

        val result = tools.editTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'accountId'")
    }

    @Test
    fun `addTransaction surfaces isError when description is missing`() = runTest {
        val args: JsonObject = buildJsonObject {
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
            put("type", "expense")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'description'")
    }

    @Test
    fun `addTransaction surfaces isError when type is missing`() = runTest {
        // type is documented upstream as optional with default 'expense' but the live
        // API rejects requests without it (HTTP 400). See AddTransactionParams.kt.
        val args: JsonObject = buildJsonObject {
            put("description", "Test Transaction")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'type'")
    }

    @Test
    fun `uploadStatement surfaces isError when statement is missing`() = runTest {
        val args: JsonObject = buildJsonObject { put("accountId", 10350) }

        val result = tools.uploadStatement(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'statement'")
    }

    @Test
    fun `addTransaction threads fromAccountId and toAccountId into params for transfer type`() = runTest {
        val captured = slot<AddTransactionParams>()
        coEvery { mockClient.addTransaction(capture(captured)) } returns buildJsonObject { put("id", 1) }
        val args: JsonObject = buildJsonObject {
            put("description", "Move funds"); put("amount", 200.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "transfer")
            put("fromAccountId", 10350)
            put("toAccountId", 10351)
        }

        tools.addTransaction(args)

        assertThat(captured.captured.fromAccountId).isEqualTo(10350)
        assertThat(captured.captured.toAccountId).isEqualTo(10351)
    }

    @Test
    fun `addTransaction resolves 'me' in sharedBill sharers and payers to the logged-in email`() = runTest {
        // The MCP client doesn't know the user's email — it just passes "me" and the
        // server substitutes the email captured at login (mocked via mockClient).
        val captured = slot<AddTransactionParams>()
        coEvery { mockClient.addTransaction(capture(captured)) } returns buildJsonObject { put("id", 1) }
        val args: JsonObject = buildJsonObject {
            put("description", "Dinner"); put("amount", 100.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "sharedBill")
            putJsonArray("sharers") {
                addJsonObject { put("email", "me"); put("amount", 60.0) }
                addJsonObject { put("email", "friend@example.com"); put("amount", 40.0) }
            }
            putJsonArray("payers") {
                addJsonObject { put("email", "me"); put("amount", 100.0) }
            }
        }

        tools.addTransaction(args)

        assertThat(captured.captured.sharers).containsExactly(
            PayerShare(email = "me@example.com", amount = 60.0),
            PayerShare(email = "friend@example.com", amount = 40.0),
        )
        assertThat(captured.captured.payers).containsExactly(
            PayerShare(email = "me@example.com", amount = 100.0),
        )
    }

    @Test
    fun `addTransaction resolves 'me' case-insensitively in loan loanedBy and borrowedBy`() = runTest {
        // Mixed-case marker still resolves — the schema documents "me" but Claude may
        // capitalize. The check is intentionally case-insensitive.
        val captured = slot<AddTransactionParams>()
        coEvery { mockClient.addTransaction(capture(captured)) } returns buildJsonObject { put("id", 1) }
        val args: JsonObject = buildJsonObject {
            put("description", "Loan to friend"); put("amount", 50.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "loan")
            put("loanedBy", "ME")
            put("borrowedBy", "friend@example.com")
        }

        tools.addTransaction(args)

        assertThat(captured.captured.loanedBy).isEqualTo("me@example.com")
        assertThat(captured.captured.borrowedBy).isEqualTo("friend@example.com")
    }

    @Test
    fun `addTransaction resolves 'me' in paidForFriend paidBy and paidFor`() = runTest {
        val captured = slot<AddTransactionParams>()
        coEvery { mockClient.addTransaction(capture(captured)) } returns buildJsonObject { put("id", 1) }
        val args: JsonObject = buildJsonObject {
            put("description", "Covered tab"); put("amount", 25.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "paidForFriend")
            put("paidBy", "me")
            put("paidFor", "friend@example.com")
        }

        tools.addTransaction(args)

        assertThat(captured.captured.paidBy).isEqualTo("me@example.com")
        assertThat(captured.captured.paidFor).isEqualTo("friend@example.com")
    }

    @Test
    fun `addTransaction rejects sharedBill when sharers does not include the current user`() = runTest {
        // No "me" anywhere in sharers — the user isn't a participant, which is
        // nonsensical for a sharedBill (it lives in the user's books).
        val args: JsonObject = buildJsonObject {
            put("description", "Dinner"); put("amount", 100.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "sharedBill")
            putJsonArray("sharers") {
                addJsonObject { put("email", "alice@example.com"); put("amount", 50.0) }
                addJsonObject { put("email", "bob@example.com"); put("amount", 50.0) }
            }
            putJsonArray("payers") {
                addJsonObject { put("email", "me"); put("amount", 100.0) }
            }
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        val text = (result.content[0] as TextContent).text
        assertThat(text).contains("sharers")
        assertThat(text).contains("\"me\"")
    }

    @Test
    fun `addTransaction rejects sharedBill when payers does not include the current user`() = runTest {
        val args: JsonObject = buildJsonObject {
            put("description", "Dinner"); put("amount", 100.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "sharedBill")
            putJsonArray("sharers") {
                addJsonObject { put("email", "me"); put("amount", 50.0) }
                addJsonObject { put("email", "alice@example.com"); put("amount", 50.0) }
            }
            putJsonArray("payers") {
                addJsonObject { put("email", "alice@example.com"); put("amount", 100.0) }
            }
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("payers")
    }

    @Test
    fun `addTransaction rejects loan when neither loanedBy nor borrowedBy is the current user`() = runTest {
        val args: JsonObject = buildJsonObject {
            put("description", "Loan between friends"); put("amount", 50.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "loan")
            put("loanedBy", "alice@example.com")
            put("borrowedBy", "bob@example.com")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        val text = (result.content[0] as TextContent).text
        assertThat(text).contains("loanedBy")
        assertThat(text).contains("borrowedBy")
    }

    @Test
    fun `addTransaction rejects loan when both loanedBy and borrowedBy are the current user`() = runTest {
        // Self-loan is meaningless — if both sides are "me", reject with a specific message.
        val args: JsonObject = buildJsonObject {
            put("description", "Self loan?"); put("amount", 50.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "loan")
            put("loanedBy", "me")
            put("borrowedBy", "me")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("cannot both be the current user")
    }

    @Test
    fun `addTransaction rejects paidForFriend when neither paidBy nor paidFor is the current user`() = runTest {
        val args: JsonObject = buildJsonObject {
            put("description", "Friend covered another friend"); put("amount", 25.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "paidForFriend")
            put("paidBy", "alice@example.com")
            put("paidFor", "bob@example.com")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        val text = (result.content[0] as TextContent).text
        assertThat(text).contains("paidBy")
        assertThat(text).contains("paidFor")
    }

    @Test
    fun `addTransaction rejects paidForFriend when both paidBy and paidFor are the current user`() = runTest {
        // Both being "me" makes it a regular expense, not paidForFriend — surface a clear hint.
        val args: JsonObject = buildJsonObject {
            put("description", "Paid for myself"); put("amount", 25.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "paidForFriend")
            put("paidBy", "me")
            put("paidFor", "me")
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("regular expense")
    }

    @Test
    fun `addTransaction surfaces isError when 'me' is used without an active session`() = runTest {
        // currentUserEmail is null → resolveMe throws → runCatching turns it into
        // isError so the LLM sees a clear message instead of a silent network failure.
        every { mockClient.currentUserEmail } returns null
        val args: JsonObject = buildJsonObject {
            put("description", "Dinner"); put("amount", 100.0); put("accountId", 10350)
            put("date", "2026-05-16"); put("type", "sharedBill")
            putJsonArray("sharers") {
                addJsonObject { put("email", "me"); put("amount", 50.0) }
                addJsonObject { put("email", "friend@example.com"); put("amount", 50.0) }
            }
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'me'")
    }
}
