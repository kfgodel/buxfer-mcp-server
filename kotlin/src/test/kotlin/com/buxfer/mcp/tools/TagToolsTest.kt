package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TagToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TagTools
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tools = TagTools(mockClient)
    }

    @Test
    fun `listTags returns JSON array with parentId preserved`() = runTest {
        val tags = listOf(
            Tag(id = 9125, name = "Tag 9125", relativeName = "tag 9125", parentId = null),
            Tag(id = 58045, name = "Tag 58045", relativeName = "tag 58045", parentId = null),
            Tag(id = 57902, name = "Tag 57902", relativeName = "tag 57902", parentId = 58046)
        )
        coEvery { mockClient.getTags() } returns tags

        val result = tools.listTags()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(3, parsed.size)
        assertEquals(58046, parsed[2].jsonObject["parentId"]!!.jsonPrimitive.int)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getTags() } throws BuxferApiException("boom")

        val result = tools.listTags()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
