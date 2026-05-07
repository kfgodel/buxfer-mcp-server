package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TagToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TagTools

    @BeforeEach
    fun setUp() {
        tools = TagTools(mockClient)
    }

    @Test
    fun `listTags returns JSON array with parentId preserved`() = runTest {
        // BuxferClient.getTags() now returns the raw JsonArray; build the test fixture to match.
        val tags = buildJsonArray {
            addJsonObject { put("id", 9125); put("name", "Tag 9125"); put("relativeName", "tag 9125"); put("parentId", JsonNull) }
            addJsonObject { put("id", 58045); put("name", "Tag 58045"); put("relativeName", "tag 58045"); put("parentId", JsonNull) }
            addJsonObject { put("id", 57902); put("name", "Tag 57902"); put("relativeName", "tag 57902"); put("parentId", 58046) }
        }
        coEvery { mockClient.getTags() } returns tags

        val result = tools.listTags()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(3)
        assertThatJson(text).inPath("$[2].parentId").isEqualTo(58046)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getTags() } throws BuxferApiException("boom")

        val result = tools.listTags()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
