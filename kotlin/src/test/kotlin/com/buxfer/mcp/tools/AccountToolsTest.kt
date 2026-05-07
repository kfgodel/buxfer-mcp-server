package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: AccountTools

    // BuxferClient.getAccounts() now returns the raw JsonArray Buxfer's response carries — the
    // tool layer no longer holds typed Account data. Build the test fixture directly as a
    // JsonArray to mirror that shape.
    private val fixtureAccounts = buildJsonArray {
        addJsonObject { put("id", 10350); put("name", "Test Account 10350"); put("bank", "Test Bank"); put("balance", 360.01); put("currency", "ARS") }
        addJsonObject { put("id", 53367); put("name", "Test Account 53367"); put("bank", "Test Bank"); put("balance", 420.01); put("currency", "ARS") }
        addJsonObject { put("id", 18803); put("name", "Test Account 18803"); put("bank", "Test Bank"); put("balance", 821.01); put("currency", "USD") }
        addJsonObject { put("id", 18804); put("name", "Test Account 18804"); put("bank", "Test Bank"); put("balance", 822.01); put("currency", "EUR") }
        addJsonObject { put("id", 18027); put("name", "Test Account 18027"); put("bank", "Test Bank"); put("balance", 45.01); put("currency", "ARS") }
    }

    @BeforeEach
    fun setUp() {
        tools = AccountTools(mockClient)
    }

    @Test
    fun `listAccounts returns JSON array of accounts`() = runTest {
        coEvery { mockClient.getAccounts() } returns fixtureAccounts

        val result = tools.listAccounts()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(5)
        assertThatJson(text).inPath("$[0].id").isEqualTo(10350)
        assertThatJson(text).inPath("$[0].balance").isEqualTo(360.01)
        assertThatJson(text).inPath("$[0].currency").isEqualTo("ARS")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getAccounts() } throws BuxferApiException("boom")

        val result = tools.listAccounts()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
