package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.AccountRef
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.Transaction
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.api.models.TransactionsResult
import com.buxfer.mcp.api.models.UploadStatementResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransactionToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TransactionTools
    private val json = Json { ignoreUnknownKeys = true }

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

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonObject
        val txArray = parsed["transactions"]!!.jsonArray
        assertEquals(5, txArray.size)
        assertEquals(5, parsed["numTransactions"]!!.jsonPrimitive.int)
        val first = txArray[0].jsonObject
        assertEquals(33040, first["id"]!!.jsonPrimitive.int)
        assertEquals("expense", first["type"]!!.jsonPrimitive.content)
        assertEquals("expense", first["transactionType"]!!.jsonPrimitive.content)
        assertEquals(false, first["isFutureDated"]!!.jsonPrimitive.boolean)
        assertEquals(false, first["isPending"]!!.jsonPrimitive.boolean)
        assertEquals(0, txArray[0].jsonObject["tagNames"]!!.jsonArray.size)
        // transfer has fromAccount/toAccount
        val transfer = txArray[3].jsonObject
        assertEquals(603017, transfer["fromAccount"]!!.jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(1100868, transfer["toAccount"]!!.jsonObject["id"]!!.jsonPrimitive.int)
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

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonObject
        assertEquals(33645, parsed["id"]!!.jsonPrimitive.int)
        assertEquals("Test Transaction", parsed["description"]!!.jsonPrimitive.content)
    }

    @Test
    fun `editTransaction passes id and fields to client`() = runTest {
        coEvery { mockClient.editTransaction(33645, any()) } returns editedTransaction
        val args: JsonObject = buildJsonObject {
            put("id", 33645)
            put("description", "Test Transaction (edited)")
            put("amount", 0.01)
            put("accountId", 10350)
            put("date", "2026-04-26")
        }

        val result = tools.editTransaction(args)

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonObject
        assertEquals(33645, parsed["id"]!!.jsonPrimitive.int)
        assertEquals("Test Transaction (edited)", parsed["description"]!!.jsonPrimitive.content)
    }

    @Test
    fun `deleteTransaction returns confirmation on success`() = runTest {
        coEvery { mockClient.deleteTransaction(33645) } returns Unit
        val args: JsonObject = buildJsonObject { put("id", 33645) }

        val result = tools.deleteTransaction(args)

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonObject
        assertTrue(parsed["deleted"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(33645, parsed["id"]!!.jsonPrimitive.int)
    }

    @Test
    fun `uploadStatement returns uploaded count and balance`() = runTest {
        coEvery { mockClient.uploadStatement(any(), any(), any()) } returns UploadStatementResult(uploaded = 15, balance = 1234.56)
        val args: JsonObject = buildJsonObject {
            put("accountId", 10350)
            put("statement", "csv-content")
        }

        val result = tools.uploadStatement(args)

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonObject
        assertEquals(15, parsed["uploaded"]!!.jsonPrimitive.int)
        assertEquals(1234.56, parsed["balance"]!!.jsonPrimitive.content.toDouble())
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getTransactions(any()) } throws BuxferApiException("boom")

        val result = tools.listTransactions(null)

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
