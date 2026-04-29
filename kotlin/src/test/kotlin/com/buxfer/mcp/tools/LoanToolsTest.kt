package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Loan
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

class LoanToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: LoanTools
    private val json = Json { ignoreUnknownKeys = true }

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

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(3, parsed.size)
        assertEquals("contact", parsed[0].jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getLoans() } throws BuxferApiException("boom")

        val result = tools.listLoans()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
