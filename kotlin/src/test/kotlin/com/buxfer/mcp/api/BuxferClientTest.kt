package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.api.models.AddTransactionParams
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        client.login("user@example.com", "password")
        assertThat(client.token).isEqualTo("test-mock-token")
    }

    @Test
    fun `getAccounts returns parsed Account list`() = runTest {
        val accounts = client.getAccounts()
        assertThat(accounts).hasSize(5)
        assertThat(accounts[0].id).isEqualTo(10350)
        assertThat(accounts[0].balance).isEqualTo(360.01)
        assertThat(accounts[0].currency).isEqualTo("ARS")
    }

    @Test
    fun `getTransactions returns parsed Transaction list`() = runTest {
        val result = client.getTransactions()
        assertThat(result.transactions).hasSize(5)
        assertThat(result.numTransactions).isEqualTo(5)
        val first = result.transactions[0]
        assertThat(first.id).isEqualTo(33040)
        assertThat(first.type).isEqualTo("expense")
        assertThat(first.transactionType).isEqualTo("expense")
        assertThat(first.expenseAmount).isEqualTo(0.01)
        assertThat(first.tagNames).isEmpty()
        assertThat(first.isFutureDated).isFalse()
        assertThat(first.isPending).isFalse()
        // transfer transaction has fromAccount/toAccount
        val transfer = result.transactions[3]
        assertThat(transfer.fromAccount?.id).isEqualTo(603017)
        assertThat(transfer.fromAccount?.name).isEqualTo("Galicia ARS")
        assertThat(transfer.toAccount?.id).isEqualTo(1100868)
    }

    @Test
    fun `addTransaction returns created Transaction`() = runTest {
        val tx = client.addTransaction(
            AddTransactionParams("Test Transaction", 0.01, 10350, "2026-04-26")
        )
        assertThat(tx.id).isEqualTo(33645)
        assertThat(tx.type).isEqualTo("expense")
    }

    @Test
    fun `editTransaction returns updated Transaction`() = runTest {
        val tx = client.editTransaction(
            33645,
            AddTransactionParams("Test Transaction (edited)", 0.01, 10350, "2026-04-26")
        )
        assertThat(tx.id).isEqualTo(33645)
        assertThat(tx.description).isEqualTo("Test Transaction (edited)")
    }

    @Test
    fun `deleteTransaction succeeds without exception`() = runTest {
        client.deleteTransaction(33645)
        // no exception = success
    }

    @Test
    fun `uploadStatement returns upload count and balance`() = runTest {
        val result = client.uploadStatement(10350, "csv-content")
        assertThat(result.uploaded).isEqualTo(15)
        assertThat(result.balance).isEqualTo(1234.56)
    }

    @Test
    fun `getTags returns parsed Tag list with parentId`() = runTest {
        val tags = client.getTags()
        assertThat(tags).hasSize(3)
        assertThat(tags[0].id).isEqualTo(9125)
        assertThat(tags[2].parentId).isEqualTo(58046)
    }

    @Test
    fun `getBudgets returns parsed Budget list`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets).hasSize(2)
        val first = budgets[0]
        assertThat(first.id).isEqualTo(58182)
        assertThat(first.name).isEqualTo("Budget 58182")
        assertThat(first.spent).isEqualTo(946905.21)
        assertThat(first.balance).isEqualTo(-896905.21)
        assertThat(first.editMode).isEqualTo("schedule_all")
        assertThat(first.periodSize).isEqualTo(1)
        assertThat(first.startDate).isEqualTo("2022-03-01")
        assertThat(first.stopDate).isNull()
        assertThat(first.budgetId).isEqualTo(58182)
        assertThat(first.type).isEqualTo(1)
        assertThat(first.tagId).isEqualTo(57904)
        assertThat(first.tag?.id).isEqualTo(57904)
        assertThat(first.tag?.name).isEqualTo("Budget Tag 57904")
        assertThat(first.isRolledOver).isEqualTo(0)
        assertThat(first.eventId).isEqualTo(34183)
    }

    @Test
    fun `getReminders returns parsed Reminder list`() = runTest {
        val reminders = client.getReminders()
        assertThat(reminders).hasSize(2)
        val first = reminders[0]
        assertThat(first.id).isEqualTo(57872)
        assertThat(first.name).isEqualTo("Reminder 57872")
        assertThat(first.description).isEqualTo("Reminder description 57872")
        assertThat(first.periodUnit).isEqualTo("month")
        assertThat(first.nextExecution).isEqualTo("2026-05-03")
        assertThat(first.dueDateDescription).isEqualTo("2026-05-03")
        assertThat(first.numDaysForDueDate).isEqualTo(7)
        assertThat(first.tags).hasSize(1)
        assertThat(first.tags?.get(0)?.id).isEqualTo(19297)
        assertThat(first.tags?.get(0)?.name).isEqualTo("Tag 19297")
        assertThat(first.editMode).isEqualTo("schedule_all")
        assertThat(first.periodSize).isEqualTo(1)
        assertThat(first.stopDate).isNull()
        assertThat(first.type).isEqualTo("expense")
        assertThat(first.transactionType).isEqualTo(3)
    }

    @Test
    fun `getGroups returns empty list from fixture`() = runTest {
        val groups = client.getGroups()
        assertThat(groups).isEmpty()
    }

    @Test
    fun `getContacts returns parsed Contact list`() = runTest {
        val contacts = client.getContacts()
        assertThat(contacts).hasSize(4)
        assertThat(contacts[0].email).isEqualTo("contact.1@example.com")
    }

    @Test
    fun `getLoans returns parsed Loan list`() = runTest {
        val loans = client.getLoans()
        assertThat(loans).hasSize(3)
        assertThat(loans[0].type).isEqualTo("contact")
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
        assertThat(ex.message).isEqualTo("Invalid token")
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
