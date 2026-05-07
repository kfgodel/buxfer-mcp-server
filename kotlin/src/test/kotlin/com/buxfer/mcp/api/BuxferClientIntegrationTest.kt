package com.buxfer.mcp.api

import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.testing.MockEngineSupport
import com.buxfer.mcp.testing.WireMockSupport
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Integration test for [BuxferClient] against an embedded WireMock serving the same
 * fixtures used by the Docker WireMock in api-recordings/. Validates that the HTTP
 * client speaks the actual wire protocol correctly — token threading, query strings,
 * form posts, and JSON deserialization — against a real network loopback.
 *
 * Complementary to [BuxferClientTest] (Ktor MockEngine). The MockEngine suite runs
 * faster and covers deserialization breadth; this suite proves the same surface
 * works against a real HTTP stack.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuxferClientIntegrationTest {

    private val wireMock: WireMockServer = WireMockSupport.newServer()
    private lateinit var client: BuxferClient

    @BeforeAll
    fun startWireMock() {
        wireMock.start()
    }

    @AfterAll
    fun stopWireMock() {
        wireMock.stop()
    }

    @BeforeEach
    fun setUp() = runBlocking {
        wireMock.resetRequests()
        client = BuxferClient(BuxferClientConfig(baseUrl = "http://localhost:${wireMock.port()}/api"))
        client.login("user@example.com", "password")
    }

    @AfterEach
    fun tearDown() {
        client.close()
        wireMock.resetRequests()
    }

    @Test
    fun `login round-trips real HTTP and stores the token for subsequent requests`() = runTest {
        // setUp() already logged in; if the token wasn't stored, the next call would throw.
        client.getAccounts()
        assertThat(wireMock.findAll(getRequestedFor(urlPathEqualTo("/api/accounts")))).hasSize(1)
    }

    @Test
    fun `getAccounts returns deserialized fixture data`() = runTest {
        val accounts = client.getAccounts()
        assertThat(accounts).hasSize(5)
        assertThatJson(accounts.toString()).inPath("$[0].id").isEqualTo(10350)
    }

    @Test
    fun `getTransactions with empty filters returns deserialized fixture data`() = runTest {
        val result = client.getTransactions()
        val text = result.toString()
        assertThatJson(text).inPath("$.numTransactions").isString().isEqualTo("5")
        assertThatJson(text).inPath("$.transactions").isArray.hasSize(5)
        assertThatJson(text).inPath("$.transactions[0].id").isEqualTo(33040)
    }

    @Test
    fun `getTransactions threads filter values into the query string`() = runTest {
        client.getTransactions(TransactionFilters(accountId = 10350, status = "cleared"))

        val captured = wireMock.findAll(getRequestedFor(urlPathEqualTo("/api/transactions"))).single()
        assertThat(captured.queryParameter("accountId").firstValue()).isEqualTo("10350")
        assertThat(captured.queryParameter("status").firstValue()).isEqualTo("cleared")
        assertThat(captured.queryParameter("token").firstValue()).isEqualTo("test-mock-token")
    }

    @Test
    fun `addTransaction posts form data and returns the created transaction`() = runTest {
        val tx = client.addTransaction(AddTransactionParams("Test Transaction", 0.01, 10350, "2026-04-26"))
        assertThat(tx.id).isEqualTo(33645)
    }

    @Test
    fun `editTransaction posts form data and returns the updated transaction`() = runTest {
        val tx = client.editTransaction(
            33645,
            AddTransactionParams("Test Transaction (edited)", 0.01, 10350, "2026-04-26")
        )
        assertThat(tx.id).isEqualTo(33645)
    }

    @Test
    fun `deleteTransaction posts form data and succeeds`() = runTest {
        client.deleteTransaction(33645)
        assertThat(wireMock.findAll(postRequestedFor(urlPathEqualTo("/api/transaction_delete")))).hasSize(1)
    }

    @Test
    fun `uploadStatement posts form data and returns the upload result`() = runTest {
        // The shared upload_statement.json fixture is an empty `{}` (the endpoint requires a real
        // statement file and is captured manually — see api-recordings/CLAUDE.md). Override the
        // WireMock stub for this test only with a representative success envelope.
        wireMock.stubFor(post(urlPathEqualTo("/api/upload_statement"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(MockEngineSupport.UPLOAD_STATEMENT_OK_BODY)))

        val result = client.uploadStatement(10350, "csv-content")
        assertThat(result.uploaded).isEqualTo(15)
    }

    @Test
    fun `getTags returns deserialized fixture data`() = runTest {
        val tags = client.getTags()
        assertThat(tags).hasSize(3)
        assertThatJson(tags.toString()).inPath("$[0].id").isEqualTo(9125)
    }

    @Test
    fun `getBudgets returns deserialized fixture data`() = runTest {
        val budgets = client.getBudgets()
        assertThat(budgets).hasSize(2)
        assertThatJson(budgets.toString()).inPath("$[0].id").isEqualTo(58182)
    }

    @Test
    fun `getReminders returns deserialized fixture data`() = runTest {
        val reminders = client.getReminders()
        assertThat(reminders).hasSize(2)
        assertThatJson(reminders.toString()).inPath("$[0].id").isEqualTo(57872)
    }

    @Test
    fun `getGroups returns deserialized fixture data`() = runTest {
        val groups = client.getGroups()
        assertThat(groups).isEmpty()
    }

    @Test
    fun `getContacts returns deserialized fixture data`() = runTest {
        val contacts = client.getContacts()
        assertThat(contacts).hasSize(4)
        assertThatJson(contacts.toString()).inPath("$[0].id").isEqualTo(4436)
    }

    @Test
    fun `getLoans returns deserialized fixture data`() = runTest {
        val loans = client.getLoans()
        assertThat(loans).hasSize(3)
    }
}
