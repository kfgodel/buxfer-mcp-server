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

class LoanToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: LoanTools

    @BeforeEach
    fun setUp() {
        tools = LoanTools(mockClient)
    }

    @Test
    fun `listLoans returns JSON array of loans`() = runTest {
        // BuxferClient.getLoans() now returns the raw JsonArray; build the test fixture to match.
        val loans = buildJsonArray {
            addJsonObject { put("entity", "Test Entity 1"); put("type", "contact"); put("balance", 5000.0); put("description", "Loan 1") }
            addJsonObject { put("entity", "Test Entity 2"); put("type", "contact"); put("balance", 103547.0); put("description", "Loan 2") }
            addJsonObject { put("entity", "Test Entity 3"); put("type", "contact"); put("balance", 49937.71); put("description", "Loan 3") }
        }
        coEvery { mockClient.getLoans() } returns loans

        val result = tools.listLoans()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(3)
        assertThatJson(text).inPath("$[0].type").isEqualTo("contact")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getLoans() } throws BuxferApiException("boom")

        val result = tools.listLoans()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
