package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.AccountRef
import com.buxfer.mcp.api.models.Transaction
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.api.models.TransactionsResult
import com.buxfer.mcp.api.models.UploadStatementResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TransactionTools

    private val fixtureTransactions = listOf(
        Transaction(id = 33040, description = "Transaction 33040", amount = 0.01, accountId = 10350,
            accountName = "Test Account", date = "2026-04-26", tags = "", type = "expense", status = "cleared",
            transactionType = "expense", expenseAmount = 0.01, tagNames = emptyList(),
            isFutureDated = false, isPending = false),
        Transaction(id = 33026, description = "Transaction 33026", amount = 0.01, accountId = 10350,
            accountName = "Test Account", date = "2026-04-26", tags = "", type = "expense", status = "cleared",
            transactionType = "expense", expenseAmount = 0.01, tagNames = emptyList(),
            isFutureDated = false, isPending = false),
        Transaction(id = 39962, description = "Transaction 39962", amount = 6.49, accountId = 12768,
            accountName = "Test Account", date = "2026-04-25", tags = "Tag 1", type = "expense", status = "pending",
            transactionType = "expense", expenseAmount = 9024.1, tagNames = listOf("Tag 1"),
            isFutureDated = false, isPending = true),
        Transaction(id = 22654, description = "Transaction 22654", amount = 50000.0, accountId = null,
            accountName = "Test Account", date = "2026-04-24", tags = "", type = "transfer", status = "cleared",
            transactionType = "transfer", expenseAmount = 0.0, tagNames = emptyList(),
            isFutureDated = false, isPending = false,
            fromAccount = AccountRef(id = 603017, name = "Galicia ARS"),
            toAccount = AccountRef(id = 1100868, name = "MercadoPago")),
        Transaction(id = 22653, description = "Transaction 22653", amount = 4.1, accountId = 18027,
            accountName = "Test Account", date = "2026-04-24", tags = "Tag 1", type = "income", status = "cleared",
            transactionType = "income", expenseAmount = -4.1, tagNames = listOf("Tag 1"),
            isFutureDated = false, isPending = false)
    )

    private val addedTransaction = Transaction(
        id = 33645, description = "Test Transaction", amount = 0.01, accountId = 10350,
        accountName = "Test Account", date = "2026-04-26", tags = "", type = "expense", status = "cleared",
        transactionType = "expense", expenseAmount = 0.01, tagNames = emptyList(),
        isFutureDated = false, isPending = false
    )

    private val editedTransaction = Transaction(
        id = 33645, description = "Test Transaction (edited)", amount = 0.01, accountId = 10350,
        accountName = "Test Account", date = "2026-04-26", tags = "", type = "expense", status = "cleared",
        transactionType = "expense", expenseAmount = 0.01, tagNames = emptyList(),
        isFutureDated = false, isPending = false
    )

    @BeforeEach
    fun setUp() {
        tools = TransactionTools(mockClient)
    }

    @Test
    fun `listTransactions returns JSON with transactions and count`() = runTest {
        coEvery { mockClient.getTransactions(any()) } returns TransactionsResult(fixtureTransactions, 5)

        val result = tools.listTransactions(null)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.transactions").isArray.hasSize(5)
        assertThatJson(text).inPath("$.numTransactions").isEqualTo(5)
        assertThatJson(text).inPath("$.transactions[0].id").isEqualTo(33040)
        assertThatJson(text).inPath("$.transactions[0].type").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].transactionType").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].isFutureDated").isEqualTo(false)
        assertThatJson(text).inPath("$.transactions[0].isPending").isEqualTo(false)
        // tagNames is omitted when empty (Json { encodeDefaults = false }) — keeps the
        // JSON Claude sees free of "field": [] noise.
        assertThatJson(text).inPath("$.transactions[0].tagNames").isAbsent()
        // transfer has fromAccount/toAccount
        assertThatJson(text).inPath("$.transactions[3].fromAccount.id").isEqualTo(603017)
        assertThatJson(text).inPath("$.transactions[3].toAccount.id").isEqualTo(1100868)
    }

    @Test
    fun `listTransactions with filters passes them to client`() = runTest {
        coEvery { mockClient.getTransactions(any()) } returns TransactionsResult(emptyList(), 0)
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
        coEvery { mockClient.addTransaction(any()) } returns addedTransaction
        val args: JsonObject = buildJsonObject {
            put("description", "Test Transaction")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
        }

        val result = tools.addTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.description").isEqualTo("Test Transaction")
    }

    @Test
    fun `editTransaction returns JSON of updated transaction`() = runTest {
        coEvery { mockClient.editTransaction(33645, any()) } returns editedTransaction
        val args: JsonObject = buildJsonObject {
            put("id", 33645)
            put("description", "Test Transaction (edited)")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
        }

        val result = tools.editTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.description").isEqualTo("Test Transaction (edited)")
    }

    @Test
    fun `deleteTransaction returns confirmation on success`() = runTest {
        coEvery { mockClient.deleteTransaction(33645) } returns Unit
        val args: JsonObject = buildJsonObject { put("id", 33645) }

        val result = tools.deleteTransaction(args)

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.deleted").isEqualTo(true)
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
    }

    @Test
    fun `uploadStatement returns uploaded count and balance`() = runTest {
        coEvery { mockClient.uploadStatement(any(), any(), any()) } returns UploadStatementResult(uploaded = 15, balance = 1234.56)
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
        }

        val result = tools.addTransaction(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'description'")
    }

    @Test
    fun `uploadStatement surfaces isError when statement is missing`() = runTest {
        val args: JsonObject = buildJsonObject { put("accountId", 10350) }

        val result = tools.uploadStatement(args)

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("'statement'")
    }
}
