package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
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
        val tags = listOf(
            Tag(id = 9125, name = "Tag 9125", relativeName = "tag 9125", parentId = null),
            Tag(id = 58045, name = "Tag 58045", relativeName = "tag 58045", parentId = null),
            Tag(id = 57902, name = "Tag 57902", relativeName = "tag 57902", parentId = 58046)
        )
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
