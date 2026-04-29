package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals

class BuxferClientTest {

    private lateinit var client: BuxferClient

    private fun mockEngine(overrides: Map<String, String> = emptyMap()) = MockEngine { request ->
        val path = request.url.encodedPath
        val body = overrides.entries.firstOrNull { path.endsWith(it.key) }?.value
            ?: when {
                path.endsWith("/login") -> TestFixtureLoader.load("login")
                path.endsWith("/accounts") -> TestFixtureLoader.load("accounts")
                path.endsWith("/transactions") -> TestFixtureLoader.load("transactions")
                path.endsWith("/transaction_add") -> TestFixtureLoader.load("transaction_add")
                path.endsWith("/transaction_edit") -> TestFixtureLoader.load("transaction_edit")
                path.endsWith("/transaction_delete") -> TestFixtureLoader.load("transaction_delete")
                path.endsWith("/upload_statement") -> """{"response":{"status":"OK","uploaded":15,"balance":1234.56}}"""
                path.endsWith("/tags") -> TestFixtureLoader.load("tags")
                path.endsWith("/budgets") -> TestFixtureLoader.load("budgets")
                path.endsWith("/reminders") -> TestFixtureLoader.load("reminders")
                path.endsWith("/groups") -> TestFixtureLoader.load("groups")
                path.endsWith("/contacts") -> TestFixtureLoader.load("contacts")
                path.endsWith("/loans") -> TestFixtureLoader.load("loans")
                else -> """{"response":{"status":"error","error":"Not found"}}"""
            }
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @BeforeEach
    fun setUp() {
        client = BuxferClient(mockEngine())
        client.token = "test-token"
    }

    @Test
    fun `login stores token on success`() = runTest {
        val loginClient = BuxferClient(mockEngine())
        loginClient.login("user@example.com", "password")
        assertEquals("test-mock-token", loginClient.token)
    }

    @Test
    fun `getAccounts returns parsed Account list`() = runTest {
        val accounts = client.getAccounts()
        assertEquals(5, accounts.size)
        assertEquals(10350, accounts[0].id)
        assertEquals(360.01, accounts[0].balance)
        assertEquals("ARS", accounts[0].currency)
    }

    @Test
    fun `getTransactions returns parsed Transaction list`() = runTest {
        val result = client.getTransactions()
        assertEquals(5, result.transactions.size)
        assertEquals(5, result.numTransactions)
        assertEquals(33040, result.transactions[0].id)
        assertEquals("expense", result.transactions[0].type)
    }

    @Test
    fun `addTransaction returns created Transaction`() = runTest {
        val tx = client.addTransaction(
            AddTransactionParams("Test Transaction", 0.01, 10350, "2026-04-26")
        )
        assertEquals(33645, tx.id)
        assertEquals("expense", tx.type)
    }

    @Test
    fun `editTransaction returns updated Transaction`() = runTest {
        val tx = client.editTransaction(
            33645,
            AddTransactionParams("Test Transaction (edited)", 0.01, 10350, "2026-04-26")
        )
        assertEquals(33645, tx.id)
        assertEquals("Test Transaction (edited)", tx.description)
    }

    @Test
    fun `deleteTransaction succeeds without exception`() = runTest {
        client.deleteTransaction(33645)
        // no exception = success
    }

    @Test
    fun `uploadStatement returns upload count and balance`() = runTest {
        val result = client.uploadStatement(10350, "csv-content")
        assertEquals(15, result.uploaded)
        assertEquals(1234.56, result.balance)
    }

    @Test
    fun `getTags returns parsed Tag list with parentId`() = runTest {
        val tags = client.getTags()
        assertEquals(3, tags.size)
        assertEquals(9125, tags[0].id)
        assertEquals(58046, tags[2].parentId)
    }

    @Test
    fun `getBudgets returns parsed Budget list`() = runTest {
        val budgets = client.getBudgets()
        assertEquals(2, budgets.size)
        assertEquals(58182, budgets[0].id)
        assertEquals("Budget 58182", budgets[0].name)
        assertEquals(946905.21, budgets[0].spent)
        assertEquals(-896905.21, budgets[0].balance)
    }

    @Test
    fun `getReminders returns parsed Reminder list`() = runTest {
        val reminders = client.getReminders()
        assertEquals(2, reminders.size)
        assertEquals(57872, reminders[0].id)
        assertEquals("Reminder 57872", reminders[0].name)
        assertEquals("Reminder description 57872", reminders[0].description)
        assertEquals("month", reminders[0].periodUnit)
    }

    @Test
    fun `getGroups returns empty list from fixture`() = runTest {
        val groups = client.getGroups()
        assertEquals(0, groups.size)
    }

    @Test
    fun `getContacts returns parsed Contact list`() = runTest {
        val contacts = client.getContacts()
        assertEquals(4, contacts.size)
        assertEquals("contact.1@example.com", contacts[0].email)
    }

    @Test
    fun `getLoans returns parsed Loan list`() = runTest {
        val loans = client.getLoans()
        assertEquals(3, loans.size)
        assertEquals("contact", loans[0].type)
    }

    @Test
    fun `throws BuxferApiException on non-OK status`() = runTest {
        val errorClient = BuxferClient(MockEngine { _ ->
            respond(
                """{"response":{"status":"error","error":"Invalid token"}}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        })
        errorClient.token = "bad-token"
        val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }
        assertEquals("Invalid token", ex.message)
    }

    @Test
    fun `throws on HTTP error response`() = runTest {
        val errorClient = BuxferClient(MockEngine { _ ->
            respond("Server Error", HttpStatusCode.InternalServerError)
        })
        errorClient.token = "test-token"
        assertThrows<BuxferApiException> { errorClient.getAccounts() }
    }
}
