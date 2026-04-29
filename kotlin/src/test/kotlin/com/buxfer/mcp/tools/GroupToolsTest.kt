package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GroupToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: GroupTools
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tools = GroupTools(mockClient)
    }

    @Test
    fun `listGroups returns empty JSON array from fixture`() = runTest {
        coEvery { mockClient.getGroups() } returns emptyList()

        val result = tools.listGroups()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(0, parsed.size)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getGroups() } throws BuxferApiException("boom")

        val result = tools.listGroups()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
