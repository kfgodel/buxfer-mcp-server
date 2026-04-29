package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Budget
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: BudgetTools

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

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(2)
        assertThatJson(text).inPath("$[0].id").isEqualTo(58182)
        assertThatJson(text).inPath("$[0].name").isEqualTo("Budget 58182")
        assertThatJson(text).inPath("$[0].spent").isEqualTo(946905.21)
        assertThatJson(text).inPath("$[0].editMode").isEqualTo("schedule_all")
        assertThatJson(text).inPath("$[0].periodSize").isEqualTo(1)
        assertThatJson(text).inPath("$[0].startDate").isEqualTo("2022-03-01")
        assertThatJson(text).inPath("$[0].tagId").isEqualTo(57904)
        assertThatJson(text).inPath("$[0].tag.id").isEqualTo(57904)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getBudgets() } throws BuxferApiException("boom")

        val result = tools.listBudgets()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
