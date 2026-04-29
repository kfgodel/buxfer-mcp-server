package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Account
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: AccountTools

    private val fixtureAccounts = listOf(
        Account(id = 10350, name = "Test Account 10350", bank = "Test Bank", balance = 360.01, currency = "ARS"),
        Account(id = 53367, name = "Test Account 53367", bank = "Test Bank", balance = 420.01, currency = "ARS"),
        Account(id = 18803, name = "Test Account 18803", bank = "Test Bank", balance = 821.01, currency = "USD"),
        Account(id = 18804, name = "Test Account 18804", bank = "Test Bank", balance = 822.01, currency = "EUR"),
        Account(id = 18027, name = "Test Account 18027", bank = "Test Bank", balance = 45.01, currency = "ARS")
    )

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
