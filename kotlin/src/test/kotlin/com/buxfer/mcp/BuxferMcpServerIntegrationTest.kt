package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.BuxferClientConfig
import com.buxfer.mcp.testing.WireMockSupport
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.modelcontextprotocol.kotlin.sdk.ExperimentalMcpApi
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.mcpClient
import io.modelcontextprotocol.kotlin.sdk.testing.ChannelTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
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
 * End-to-end integration test exercising every layer of the MCP pipeline:
 *
 *   SDK Client ──ChannelTransport──▶ BuxferMcpServer ──tool dispatch──▶ BuxferClient ──HTTP──▶ WireMock
 *
 * Each scenario calls a real `mcpClient` (from kotlin-sdk-client) against a real `Server`
 * (constructed by `BuxferMcpServer`), connected via `ChannelTransport.createLinkedPair()`
 * (the SDK's published in-memory transport from kotlin-sdk-testing). Tool handlers run a
 * real `BuxferClient` whose Ktor HTTP requests hit an embedded WireMock that serves the
 * captured Buxfer fixtures.
 *
 * This is the showcase test — a future reader should be able to follow the round-trip
 * top to bottom.
 *
 * Scenarios:
 *   | Scenario                        | Layer it exercises                                       |
 *   |---------------------------------|----------------------------------------------------------|
 *   | listTools                       | Initialize handshake + tools/list path                   |
 *   | list_accounts round-trip        | Full pipeline, deserialization → re-encode               |
 *   | filter threading                | Args → query params plumbing via WireMock journal        |
 *   | add_transaction form post       | POST/form path                                           |
 *   | error envelope                  | BuxferApiException → isError surface                     |
 */
@OptIn(ExperimentalMcpApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuxferMcpServerIntegrationTest {

    private val wireMock: WireMockServer = WireMockSupport.newServer()
    private lateinit var httpClient: BuxferClient

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
        wireMock.resetAll()  // re-loads the JSON mappings AND clears the request journal
        httpClient = BuxferClient(BuxferClientConfig(baseUrl = "http://localhost:${wireMock.port()}/api"))
        httpClient.login("user@example.com", "password")
    }

    @AfterEach
    fun tearDown() {
        httpClient.close()
        wireMock.resetAll()
    }

    /**
     * Wire a fresh server↔client pair via in-memory ChannelTransport. The launched server job
     * is bound to `runTest`'s [TestScope] and is cleaned up when the test scope completes —
     * no explicit `cancel()` needed.
     */
    private suspend fun TestScope.launchMcpClient(): Client {
        val (clientTransport, serverTransport) = ChannelTransport.createLinkedPair()
        launch { BuxferMcpServer(httpClient).start(serverTransport) }
        return mcpClient(Implementation("integration-test", "0.0.0"), transport = clientTransport)
    }

    @Test
    fun `listTools returns the 12 registered MCP tools`() = runTest {
        val client = launchMcpClient()

        val result = client.listTools()

        // The SDK Client did the initialize handshake, then sent tools/list — exercising
        // the full JSON-RPC pipeline including ChannelTransport framing on both sides.
        assertThat(result.tools.map { it.name }).containsExactlyInAnyOrder(
            "buxfer_list_accounts",
            "buxfer_list_transactions",
            "buxfer_add_transaction",
            "buxfer_edit_transaction",
            "buxfer_delete_transaction",
            "buxfer_upload_statement",
            "buxfer_list_tags",
            "buxfer_list_budgets",
            "buxfer_list_reminders",
            "buxfer_list_groups",
            "buxfer_list_contacts",
            "buxfer_list_loans"
        )
        client.close()
    }

    @Test
    fun `buxfer_list_accounts round-trips fixture data through every layer`() = runTest {
        val client = launchMcpClient()

        val result = client.callTool("buxfer_list_accounts", emptyMap())

        // The tool ran the actual BuxferClient.getAccounts() → HTTP → WireMock (serving
        // accounts.json) → deserialize → JSON-encode for the MCP response.
        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$[0].id").isEqualTo(10350)
        assertThatJson(text).inPath("$").isArray.hasSize(5)
        client.close()
    }

    @Test
    fun `tool arguments are threaded into the HTTP request query string`() = runTest {
        val client = launchMcpClient()

        client.callTool("buxfer_list_transactions", mapOf("accountId" to 10350))

        // Validates the full plumbing: MCP arguments → tool params → BuxferClient query
        // params → wire request. WireMock's request journal proves the value made it to HTTP.
        val captured = wireMock.findAll(getRequestedFor(urlPathEqualTo("/api/transactions"))).single()
        assertThat(captured.queryParameter("accountId").firstValue()).isEqualTo("10350")
        client.close()
    }

    @Test
    fun `buxfer_add_transaction posts form data and returns the created transaction`() = runTest {
        val client = launchMcpClient()

        val result = client.callTool("buxfer_add_transaction", mapOf(
            "description" to "Test Transaction",
            "amount" to 0.01,
            "accountId" to 10350,
            "date" to "2026-04-26"
        ))

        // Exercises the POST/form path of BuxferClient end-to-end through MCP.
        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(33645)
        client.close()
    }

    @Test
    fun `tool surfaces isError when the API returns an error envelope`() = runTest {
        // Per-test override: replace the /api/transactions stub with one that returns the
        // Buxfer "error" envelope. .atPriority(1) wins over the priority-5 default mappings.
        wireMock.stubFor(get(urlPathEqualTo("/api/transactions"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""{"response":{"status":"error","error":"boom"}}""")))

        val client = launchMcpClient()

        val result = client.callTool("buxfer_list_transactions", emptyMap())

        // BuxferClient throws BuxferApiException; the tool catches it and surfaces an
        // MCP-level error result so Claude sees it instead of the protocol blowing up.
        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("boom")
        client.close()
    }
}
