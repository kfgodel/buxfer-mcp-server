package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import io.mockk.mockk
import io.mockk.verify
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class McpToolTest {

    @Serializable
    private data class Sample(val id: Int, val label: String, val maybe: String? = null)

    private val log = mockk<Logger>(relaxed = true)

    @Test
    fun `mcpTool returns CallToolResult wrapping encoded value`() = runTest {
        val result = mcpTool("test_tool", log) { Sample(id = 7, label = "hello") }

        assertThat(result.isError).isNotEqualTo(true)
        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.id").isEqualTo(7)
        assertThatJson(text).inPath("$.label").isEqualTo("hello")
    }

    @Test
    fun `mcpTool surfaces isError when block throws`() = runTest {
        val result = mcpTool<Sample>("test_tool", log) { throw BuxferApiException("boom") }

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).isEqualTo("Error: boom")
    }

    @Test
    fun `mcpTool logs tool name at INFO on entry`() = runTest {
        mcpTool("test_tool", log) { Sample(id = 1, label = "x") }

        verify { log.info("tool={}", "test_tool") }
    }

    @Test
    fun `mcpTool logs at ERROR with the exception when block throws`() = runTest {
        val boom = BuxferApiException("boom")

        mcpTool<Sample>("test_tool", log) { throw boom }

        verify { log.error("tool={} failed", "test_tool", boom) }
    }

    @Test
    fun `mcpTool encoded JSON honors encodeDefaults = false`() = runTest {
        val result = mcpTool("test_tool", log) { Sample(id = 1, label = "x") }

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).inPath("$.maybe").isAbsent()
    }
}
