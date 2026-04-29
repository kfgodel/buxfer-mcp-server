package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Reminder
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
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
        val reminderTag = Tag(id = 19297, name = "Tag 19297", relativeName = "tag 19297", parentId = 58082)
        val reminders = listOf(
            Reminder(id = 57872, name = "Reminder 57872", description = "Reminder description 57872",
                startDate = "2026-02-03", periodUnit = "month", amount = 3.44, accountId = 13872,
                nextExecution = "2026-05-03", dueDateDescription = "2026-05-03", numDaysForDueDate = 7,
                tags = listOf(reminderTag), editMode = "schedule_all", periodSize = 1, stopDate = null,
                type = "expense", transactionType = 3),
            Reminder(id = 6886, name = "Reminder 6886", description = "Reminder description 6886",
                startDate = "2026-03-16", periodUnit = "month", amount = 39612.63, accountId = 605)
        )
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
