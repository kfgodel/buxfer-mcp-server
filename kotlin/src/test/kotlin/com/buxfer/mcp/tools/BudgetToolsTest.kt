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
import kotlinx.serialization.json.putJsonObject
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
        // BuxferClient.getBudgets() now returns the raw JsonArray; build the test fixture to match.
        // The tool layer doesn't run validation, so a partial second record is fine here.
        val budgets = buildJsonArray {
            addJsonObject {
                put("id", 58182); put("name", "Budget 58182"); put("limit", 50000.0)
                put("spent", 946905.21); put("balance", -896905.21)
                put("period", "Apr"); put("periodUnit", "month"); put("editMode", "schedule_all")
                put("periodSize", 1); put("startDate", "2022-03-01")
                put("budgetId", 58182); put("type", 1)
                put("tagId", 57904)
                putJsonObject("tag") {
                    put("id", 57904); put("name", "Budget Tag 57904")
                    put("relativeName", "budget tag 57904"); put("parentId", 58046)
                }
                put("isRolledOver", 0); put("eventId", 34183)
            }
            addJsonObject { put("id", 58183) }  // partial; only used for hasSize(2)
        }
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
