package com.buxfer.mcp.api

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.testing.MockEngineSupport
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index.atIndex
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class BuxferClientTest {

    private lateinit var client: BuxferClient

    @BeforeEach
    fun setUp() = runBlocking {
        client = BuxferClient(BuxferClientConfig(engine = MockEngineSupport.newEngine()))
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
    fun `getAccounts returns JsonArray of account objects`() = runTest {
        val accounts = client.getAccounts()
        assertThat(accounts).hasSize(5)
        val text = accounts.toString()
        assertThatJson(text).inPath("$[0].id").isEqualTo(10350)
        assertThatJson(text).inPath("$[0].balance").isEqualTo(360.01)
        assertThatJson(text).inPath("$[0].currency").isEqualTo("ARS")
    }

    @Test
    fun `getAccounts logs schema-drift warning on missing required field`() = runTest {
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        val logger = LoggerFactory.getLogger(BuxferClient::class.java) as Logger
        logger.addAppender(appender)
        try {
            // Drop the required `currency` field from the fixture so the strict validator fires.
            BuxferClient(BuxferClientConfig(engine = MockEngineSupport.newEngine(overrides = mapOf(
                "/accounts" to """{"response":{"status":"OK","accounts":[{"id":1,"name":"x","bank":"y","balance":0.0}]}}"""
            )))).use { c ->
                c.login("user@example.com", "password")
                c.getAccounts()  // returns the JsonArray; warning logs as a side effect, no throw
            }

            val warning = appender.list.find { it.level == Level.WARN && it.formattedMessage.contains("/accounts") }
            assertThat(warning).isNotNull
            assertThat(warning!!.formattedMessage).contains("currency")
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `getTransactions deserializes core Transaction scalars`() = runTest {
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
    }

    @Test
    fun `getTransactions deserializes nested transfer accounts`() = runTest {
        val result = client.getTransactions()
        assertThat(result.transactions)
            .hasSize(5)
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
    fun `getTags returns JsonArray of tag objects with parentId`() = runTest {
        val tags = client.getTags()
        assertThat(tags).hasSize(3)
        val text = tags.toString()
        assertThatJson(text).inPath("$[0].id").isEqualTo(9125)
        assertThatJson(text).inPath("$[2].parentId").isEqualTo(58046)
    }

    @Test
    fun `getBudgets deserializes Budget scalar fields`() = runTest {
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
                assertThat(it.isRolledOver).isEqualTo(0)
                assertThat(it.eventId).isEqualTo(34183)
            }, atIndex(0))
    }

    @Test
    fun `getBudgets deserializes nested Budget tag reference`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets)
            .hasSize(2)
            .satisfies({
                assertThat(it.tagId).isEqualTo(57904)
                assertThat(it.tag?.id).isEqualTo(57904)
                assertThat(it.tag?.name).isEqualTo("Budget Tag 57904")
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
    fun `getContacts returns JsonArray of contact objects`() = runTest {
        val contacts = client.getContacts()
        assertThat(contacts).hasSize(4)
        assertThatJson(contacts.toString()).inPath("$[0].email").isEqualTo("contact.1@example.com")
    }

    @Test
    fun `getLoans returns parsed Loan list`() = runTest {
        val loans = client.getLoans()
        assertThat(loans)
            .hasSize(3)
            .satisfies({ assertThat(it.type).isEqualTo("contact") }, atIndex(0))
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
}
