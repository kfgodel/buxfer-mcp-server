package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Budget
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: BudgetTools
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tools = BudgetTools(mockClient)
    }

    @Test
    fun `listBudgets returns JSON array of budgets`() = runTest {
        val tag = Tag(id = 57904, name = "Budget Tag 57904", relativeName = "budget tag 57904", parentId = 58046)
        val budgets = listOf(
            Budget(id = 58182, name = "Budget 58182", limit = 50000.0, spent = 946905.21, balance = -896905.21,
                period = "Apr", periodUnit = "month", editMode = "schedule_all", periodSize = 1,
                startDate = "2022-03-01", stopDate = null, budgetId = 58182, type = 1,
                tagId = 57904, tag = tag, isRolledOver = 0, eventId = 34183),
            Budget(id = 58183, name = "Budget 58183", limit = 40000.0, spent = 65700.0, balance = -25700.0,
                period = "Apr", periodUnit = "month")
        )
        coEvery { mockClient.getBudgets() } returns budgets

        val result = tools.listBudgets()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(2, parsed.size)
        val first = parsed[0].jsonObject
        assertEquals(58182, first["id"]!!.jsonPrimitive.int)
        assertEquals("Budget 58182", first["name"]!!.jsonPrimitive.content)
        assertEquals(946905.21, first["spent"]!!.jsonPrimitive.double)
        assertEquals("schedule_all", first["editMode"]!!.jsonPrimitive.content)
        assertEquals(1, first["periodSize"]!!.jsonPrimitive.int)
        assertEquals("2022-03-01", first["startDate"]!!.jsonPrimitive.content)
        assertEquals(57904, first["tagId"]!!.jsonPrimitive.int)
        assertEquals(57904, first["tag"]!!.jsonObject["id"]!!.jsonPrimitive.int)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getBudgets() } throws BuxferApiException("boom")

        val result = tools.listBudgets()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
