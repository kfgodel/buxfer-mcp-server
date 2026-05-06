package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.BuxferClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index.atIndex
import org.junit.jupiter.api.AfterEach
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
    fun setUp() = runBlocking {
        client = BuxferClient(BuxferClientConfig(engine = mockEngine()))
        client.login("user@example.com", "password")
    }

    @AfterEach
    fun tearDown() {
        client.close()
    }

    @Test
    fun `login stores token from response and attaches it to subsequent requests`() = runTest {
        val capturedTokens = mutableListOf<String?>()
        val engine = MockEngine { request ->
            capturedTokens += request.url.parameters["token"]
            val body = if (request.url.encodedPath.endsWith("/login")) TestFixtureLoader.load("login")
                       else TestFixtureLoader.load("accounts")
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { freshClient ->
            freshClient.login("user@example.com", "password")
            freshClient.getAccounts()

            // First request is /login (no token); second is /accounts carrying the token from the login response.
            assertThat(capturedTokens).containsExactly(null, "test-mock-token")
        }
    }

    @Test
    fun `getAccounts returns parsed Account list`() = runTest {
        val accounts = client.getAccounts()
        assertThat(accounts)
            .hasSize(5)
            .satisfies({
                assertThat(it.id).isEqualTo(10350)
                assertThat(it.balance).isEqualTo(360.01)
                assertThat(it.currency).isEqualTo("ARS")
            }, atIndex(0))
    }

    @Test
    fun `getTransactions returns parsed Transaction list`() = runTest {
        val result = client.getTransactions()
        assertThat(result.numTransactions).isEqualTo(5)
        assertThat(result.transactions)
            .hasSize(5)
            .satisfies({
                assertThat(it.id).isEqualTo(33040)
                assertThat(it.type).isEqualTo("expense")
                assertThat(it.transactionType).isEqualTo("expense")
                assertThat(it.expenseAmount).isEqualTo(0.01)
                assertThat(it.tagNames).isEmpty()
                assertThat(it.isFutureDated).isFalse()
                assertThat(it.isPending).isFalse()
            }, atIndex(0))
            // transfer transaction has fromAccount/toAccount
            .satisfies({
                assertThat(it.fromAccount?.id).isEqualTo(603017)
                assertThat(it.fromAccount?.name).isEqualTo("Galicia ARS")
                assertThat(it.toAccount?.id).isEqualTo(1100868)
            }, atIndex(3))
    }

    @Test
    fun `addTransaction returns created Transaction`() = runTest {
        val tx = client.addTransaction(
            AddTransactionParams("Test Transaction", 0.01, 10350, "2026-04-26")
        )
        assertThat(tx).satisfies({
            assertThat(it.id).isEqualTo(33645)
            assertThat(it.type).isEqualTo("expense")
        })
    }

    @Test
    fun `editTransaction returns updated Transaction`() = runTest {
        val tx = client.editTransaction(
            33645,
            AddTransactionParams("Test Transaction (edited)", 0.01, 10350, "2026-04-26")
        )
        assertThat(tx).satisfies({
            assertThat(it.id).isEqualTo(33645)
            assertThat(it.description).isEqualTo("Test Transaction (edited)")
        })
    }

    @Test
    fun `deleteTransaction succeeds without exception`() = runTest {
        client.deleteTransaction(33645)
        // no exception = success
    }

    @Test
    fun `uploadStatement returns upload count and balance`() = runTest {
        val result = client.uploadStatement(10350, "csv-content")
        assertThat(result).satisfies({
            assertThat(it.uploaded).isEqualTo(15)
            assertThat(it.balance).isEqualTo(1234.56)
        })
    }

    @Test
    fun `getTags returns parsed Tag list with parentId`() = runTest {
        val tags = client.getTags()
        assertThat(tags)
            .hasSize(3)
            .satisfies({ assertThat(it.id).isEqualTo(9125) }, atIndex(0))
            .satisfies({ assertThat(it.parentId).isEqualTo(58046) }, atIndex(2))
    }

    @Test
    fun `getBudgets returns parsed Budget list`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets)
            .hasSize(2)
            .satisfies({
                assertThat(it.id).isEqualTo(58182)
                assertThat(it.name).isEqualTo("Budget 58182")
                assertThat(it.spent).isEqualTo(946905.21)
                assertThat(it.balance).isEqualTo(-896905.21)
                assertThat(it.editMode).isEqualTo("schedule_all")
                assertThat(it.periodSize).isEqualTo(1)
                assertThat(it.startDate).isEqualTo("2022-03-01")
                assertThat(it.stopDate).isNull()
                assertThat(it.budgetId).isEqualTo(58182)
                assertThat(it.type).isEqualTo(1)
                assertThat(it.tagId).isEqualTo(57904)
                assertThat(it.tag?.id).isEqualTo(57904)
                assertThat(it.tag?.name).isEqualTo("Budget Tag 57904")
                assertThat(it.isRolledOver).isEqualTo(0)
                assertThat(it.eventId).isEqualTo(34183)
            }, atIndex(0))
    }

    @Test
    fun `getReminders returns parsed Reminder list`() = runTest {
        val reminders = client.getReminders()
        assertThat(reminders)
            .hasSize(2)
            .satisfies({
                assertThat(it.id).isEqualTo(57872)
                assertThat(it.name).isEqualTo("Reminder 57872")
                assertThat(it.description).isEqualTo("Reminder description 57872")
                assertThat(it.periodUnit).isEqualTo("month")
                assertThat(it.nextExecution).isEqualTo("2026-05-03")
                assertThat(it.dueDateDescription).isEqualTo("2026-05-03")
                assertThat(it.numDaysForDueDate).isEqualTo(7)
                assertThat(it.tags).hasSize(1)
                assertThat(it.tags?.get(0)?.id).isEqualTo(19297)
                assertThat(it.tags?.get(0)?.name).isEqualTo("Tag 19297")
                assertThat(it.editMode).isEqualTo("schedule_all")
                assertThat(it.periodSize).isEqualTo(1)
                assertThat(it.stopDate).isNull()
                assertThat(it.type).isEqualTo("expense")
                assertThat(it.transactionType).isEqualTo(3)
            }, atIndex(0))
    }

    @Test
    fun `getGroups returns empty list from fixture`() = runTest {
        val groups = client.getGroups()
        assertThat(groups).isEmpty()
    }

    @Test
    fun `getContacts returns parsed Contact list`() = runTest {
        val contacts = client.getContacts()
        assertThat(contacts)
            .hasSize(4)
            .satisfies({ assertThat(it.email).isEqualTo("contact.1@example.com") }, atIndex(0))
    }

    @Test
    fun `getLoans returns parsed Loan list`() = runTest {
        val loans = client.getLoans()
        assertThat(loans)
            .hasSize(3)
            .satisfies({ assertThat(it.type).isEqualTo("contact") }, atIndex(0))
    }

    @Test
    fun `throws BuxferApiException on non-OK status`() = runTest {
        BuxferClient(BuxferClientConfig(engine = mockEngine(overrides = mapOf(
            "/accounts" to """{"response":{"status":"error","error":"Invalid token"}}"""
        )))).use { errorClient ->
            errorClient.login("user@example.com", "password")
            val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }
            assertThat(ex.message).isEqualTo("Invalid token")
        }
    }

    @Test
    fun `custom baseUrl is used for all requests`() = runTest {
        val capturedHosts = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedHosts += request.url.host
            val body = if (request.url.encodedPath.endsWith("/login")) TestFixtureLoader.load("login")
                       else TestFixtureLoader.load("accounts")
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        BuxferClient(BuxferClientConfig(engine = engine, baseUrl = "https://custom.example.test/api")).use { customClient ->
            customClient.login("user@example.com", "password")
            customClient.getAccounts()
            assertThat(capturedHosts).containsOnly("custom.example.test")
        }
    }

    @Test
    fun `parse error surfaces field, type, and endpoint context`() = runTest {
        // Account.id is the identity field — required, no default. If the API drops it,
        // kotlinx.serialization throws SerializationException naming the missing field
        // and type, and BuxferClient.traced wraps it as BuxferApiException with method+path
        // context. Together that's enough to diagnose real API drift on sight.
        val engine = mockEngine(overrides = mapOf(
            "/accounts" to """{"response":{"status":"OK","accounts":[{"name":"x"}]}}"""
        ))
        BuxferClient(BuxferClientConfig(engine = engine)).use { parseClient ->
            parseClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { parseClient.getAccounts() }

            assertThat(ex.message)
                .contains("GET /accounts")
                .contains("id")
                .contains("Account")
            assertThat(ex.cause).isInstanceOf(kotlinx.serialization.SerializationException::class.java)
        }
    }

    @Test
    fun `throws on HTTP error response`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/login"))
                respond(TestFixtureLoader.load("login"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond("Server Error", HttpStatusCode.InternalServerError)
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { errorClient ->
            errorClient.login("user@example.com", "password")
            assertThrows<BuxferApiException> { errorClient.getAccounts() }
        }
    }
}
