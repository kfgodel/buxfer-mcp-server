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

class ContactToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: ContactTools

    @BeforeEach
    fun setUp() {
        tools = ContactTools(mockClient)
    }

    @Test
    fun `listContacts returns JSON array of contacts`() = runTest {
        // BuxferClient.getContacts() now returns the raw JsonArray; build the test fixture to match.
        val contacts = buildJsonArray {
            addJsonObject { put("id", 4436); put("name", "Contact 1"); put("email", "contact.1@example.com"); put("balance", 0.0) }
            addJsonObject { put("id", 8953); put("name", "Contact 2"); put("email", "contact.2@example.com"); put("balance", 5000.0) }
            addJsonObject { put("id", 11481); put("name", "Contact 3"); put("email", "contact.3@example.com"); put("balance", 0.0) }
            addJsonObject { put("id", 31608); put("name", "Contact 4"); put("email", "contact.4@example.com"); put("balance", 0.0) }
        }
        coEvery { mockClient.getContacts() } returns contacts

        val result = tools.listContacts()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(4)
        assertThatJson(text).inPath("$[0].email").isEqualTo("contact.1@example.com")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getContacts() } throws BuxferApiException("boom")

        val result = tools.listContacts()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
