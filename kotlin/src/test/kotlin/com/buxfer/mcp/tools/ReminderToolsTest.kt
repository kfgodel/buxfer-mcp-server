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
import kotlinx.serialization.json.putJsonArray
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReminderToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: ReminderTools

    @BeforeEach
    fun setUp() {
        tools = ReminderTools(mockClient)
    }

    @Test
    fun `listReminders returns JSON array of reminders`() = runTest {
        // BuxferClient.getReminders() now returns the raw JsonArray; build the test fixture to match.
        // The tool layer doesn't run validation, so a partial second record is fine here.
        val reminders = buildJsonArray {
            addJsonObject {
                put("id", 57872); put("name", "Reminder 57872"); put("description", "Reminder description 57872")
                put("startDate", "2026-02-03"); put("periodUnit", "month"); put("periodSize", 1)
                put("amount", 3.44); put("accountId", 13872)
                put("nextExecution", "2026-05-03"); put("dueDateDescription", "2026-05-03")
                put("numDaysForDueDate", 7)
                putJsonArray("tags") {
                    addJsonObject {
                        put("id", 19297); put("name", "Tag 19297")
                        put("relativeName", "tag 19297"); put("parentId", 58082)
                    }
                }
                put("editMode", "schedule_all"); put("stopDate", JsonNull)
                put("type", "expense"); put("transactionType", 3)
            }
            addJsonObject { put("id", 6886) }  // partial; only used for hasSize(2)
        }
        coEvery { mockClient.getReminders() } returns reminders

        val result = tools.listReminders()

        val text = (result.content[0] as TextContent).text
        assertThatJson(text).isArray.hasSize(2)
        assertThatJson(text).inPath("$[0].id").isEqualTo(57872)
        assertThatJson(text).inPath("$[0].description").isEqualTo("Reminder description 57872")
        assertThatJson(text).inPath("$[0].periodUnit").isEqualTo("month")
        assertThatJson(text).inPath("$[0].nextExecution").isEqualTo("2026-05-03")
        assertThatJson(text).inPath("$[0].numDaysForDueDate").isEqualTo(7)
        assertThatJson(text).inPath("$[0].tags[0].id").isEqualTo(19297)
        assertThatJson(text).inPath("$[0].type").isEqualTo("expense")
        assertThatJson(text).inPath("$[0].transactionType").isEqualTo(3)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getReminders() } throws BuxferApiException("boom")

        val result = tools.listReminders()

        assertThat(result.isError).isTrue()
        assertThat((result.content[0] as TextContent).text).contains("Error: boom")
    }
}
