package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GroupToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: GroupTools

    @BeforeEach
    fun setUp() {
        tools = GroupTools(mockClient)
    }

    @Test
    fun `listGroups returns empty JSON array from fixture`() = runTest {
        coEvery { mockClient.getGroups() } returns JsonArray(emptyList())

        val result = tools.listGroups()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(0)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getGroups() } throws BuxferApiException("boom")

        val result = tools.listGroups()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
