package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Loan
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
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
        val loans = listOf(
            Loan(entity = "Test Entity 1", type = "contact", balance = 5000.0, description = "Loan 1"),
            Loan(entity = "Test Entity 2", type = "contact", balance = 103547.0, description = "Loan 2"),
            Loan(entity = "Test Entity 3", type = "contact", balance = 49937.71, description = "Loan 3")
        )
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
