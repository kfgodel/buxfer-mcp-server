package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Contact
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContactToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: ContactTools
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tools = ContactTools(mockClient)
    }

    @Test
    fun `listContacts returns JSON array of contacts`() = runTest {
        val contacts = listOf(
            Contact(id = 4436, name = "Contact 1", email = "contact.1@example.com", balance = 0.0),
            Contact(id = 8953, name = "Contact 2", email = "contact.2@example.com", balance = 5000.0),
            Contact(id = 11481, name = "Contact 3", email = "contact.3@example.com", balance = 0.0),
            Contact(id = 31608, name = "Contact 4", email = "contact.4@example.com", balance = 0.0)
        )
        coEvery { mockClient.getContacts() } returns contacts

        val result = tools.listContacts()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(4, parsed.size)
        assertEquals("contact.1@example.com", parsed[0].jsonObject["email"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getContacts() } throws BuxferApiException("boom")

        val result = tools.listContacts()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
