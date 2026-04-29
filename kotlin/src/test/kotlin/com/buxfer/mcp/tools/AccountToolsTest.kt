package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Account
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccountToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: AccountTools
    private val json = Json { ignoreUnknownKeys = true }

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

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(5, parsed.size)
        assertEquals(10350, parsed[0].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals(360.01, parsed[0].jsonObject["balance"]!!.jsonPrimitive.double)
        assertEquals("ARS", parsed[0].jsonObject["currency"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getAccounts() } throws BuxferApiException("boom")

        val result = tools.listAccounts()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
