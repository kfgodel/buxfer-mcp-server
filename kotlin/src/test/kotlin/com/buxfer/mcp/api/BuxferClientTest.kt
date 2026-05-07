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
    fun `getTransactions returns JsonObject with transactions array and numTransactions`() = runTest {
        val result = client.getTransactions()
        val text = result.toString()
        // numTransactions is forwarded as the wire string (Buxfer quotes it).
        assertThatJson(text).inPath("$.numTransactions").isString().isEqualTo("5")
        assertThatJson(text).inPath("$.transactions").isArray.hasSize(5)
        assertThatJson(text).inPath("$.transactions[0].id").isEqualTo(33040)
        assertThatJson(text).inPath("$.transactions[0].type").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].transactionType").isEqualTo("expense")
        assertThatJson(text).inPath("$.transactions[0].expenseAmount").isEqualTo(0.01)
        assertThatJson(text).inPath("$.transactions[0].isFutureDated").isEqualTo(false)
        assertThatJson(text).inPath("$.transactions[0].isPending").isEqualTo(false)
    }

    @Test
    fun `getTransactions returns JsonObject with nested transfer accounts on transfer records`() = runTest {
        val result = client.getTransactions()
        val text = result.toString()
        assertThatJson(text).inPath("$.transactions[3].fromAccount.id").isEqualTo(603017)
        assertThatJson(text).inPath("$.transactions[3].fromAccount.name").isEqualTo("Galicia ARS")
        assertThatJson(text).inPath("$.transactions[3].toAccount.id").isEqualTo(1100868)
    }

    @Test
    fun `addTransaction returns JsonObject of created transaction`() = runTest {
        val tx = client.addTransaction(
            AddTransactionParams("Test Transaction", 0.01, 10350, "2026-04-26")
        )
        val text = tx.toString()
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.type").isEqualTo("expense")
    }

    @Test
    fun `editTransaction returns JsonObject of updated transaction`() = runTest {
        val tx = client.editTransaction(
            33645,
            AddTransactionParams("Test Transaction (edited)", 0.01, 10350, "2026-04-26")
        )
        val text = tx.toString()
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        assertThatJson(text).inPath("$.description").isEqualTo("Test Transaction (edited)")
    }

    @Test
    fun `deleteTransaction succeeds without exception`() = runTest {
        client.deleteTransaction(33645)
        // no exception = success
    }

    @Test
    fun `uploadStatement returns JsonObject with upload count and balance`() = runTest {
        val result = client.uploadStatement(10350, "csv-content")
        val text = result.toString()
        assertThatJson(text).inPath("$.uploaded").isEqualTo(15)
        assertThatJson(text).inPath("$.balance").isEqualTo(1234.56)
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
    fun `getBudgets returns JsonArray with Budget scalar fields`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets).hasSize(2)
        val text = budgets.toString()
        assertThatJson(text).inPath("$[0].id").isEqualTo(58182)
        assertThatJson(text).inPath("$[0].name").isEqualTo("Budget 58182")
        assertThatJson(text).inPath("$[0].spent").isEqualTo(946905.21)
        assertThatJson(text).inPath("$[0].balance").isEqualTo(-896905.21)
        assertThatJson(text).inPath("$[0].editMode").isEqualTo("schedule_all")
        assertThatJson(text).inPath("$[0].periodSize").isEqualTo(1)
        assertThatJson(text).inPath("$[0].startDate").isEqualTo("2022-03-01")
        assertThatJson(text).inPath("$[0].stopDate").isNull()
        assertThatJson(text).inPath("$[0].budgetId").isEqualTo(58182)
        assertThatJson(text).inPath("$[0].type").isEqualTo(1)
        assertThatJson(text).inPath("$[0].isRolledOver").isEqualTo(0)
        assertThatJson(text).inPath("$[0].eventId").isEqualTo(34183)
    }

    @Test
    fun `getBudgets returns JsonArray with nested Budget tag reference`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets).hasSize(2)
        val text = budgets.toString()
        assertThatJson(text).inPath("$[0].tagId").isEqualTo(57904)
        assertThatJson(text).inPath("$[0].tag.id").isEqualTo(57904)
        assertThatJson(text).inPath("$[0].tag.name").isEqualTo("Budget Tag 57904")
    }

    @Test
    fun `getReminders returns JsonArray of reminder objects`() = runTest {
        val reminders = client.getReminders()
        assertThat(reminders).hasSize(2)
        val text = reminders.toString()
        assertThatJson(text).inPath("$[0].id").isEqualTo(57872)
        assertThatJson(text).inPath("$[0].name").isEqualTo("Reminder 57872")
        assertThatJson(text).inPath("$[0].description").isEqualTo("Reminder description 57872")
        assertThatJson(text).inPath("$[0].periodUnit").isEqualTo("month")
        assertThatJson(text).inPath("$[0].nextExecution").isEqualTo("2026-05-03")
        assertThatJson(text).inPath("$[0].dueDateDescription").isEqualTo("2026-05-03")
        assertThatJson(text).inPath("$[0].numDaysForDueDate").isEqualTo(7)
        assertThatJson(text).inPath("$[0].tags").isArray.hasSize(1)
        assertThatJson(text).inPath("$[0].tags[0].id").isEqualTo(19297)
        assertThatJson(text).inPath("$[0].tags[0].name").isEqualTo("Tag 19297")
        assertThatJson(text).inPath("$[0].editMode").isEqualTo("schedule_all")
        assertThatJson(text).inPath("$[0].periodSize").isEqualTo(1)
        assertThatJson(text).inPath("$[0].stopDate").isNull()
        assertThatJson(text).inPath("$[0].type").isEqualTo("expense")
        assertThatJson(text).inPath("$[0].transactionType").isEqualTo(3)
    }

    @Test
    fun `getGroups returns empty JsonArray from fixture`() = runTest {
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
    fun `getLoans returns JsonArray of loan objects`() = runTest {
        val loans = client.getLoans()
        assertThat(loans).hasSize(3)
        assertThatJson(loans.toString()).inPath("$[0].type").isEqualTo("contact")
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
