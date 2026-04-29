package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Reminder
import com.buxfer.mcp.api.models.Tag
import io.mockk.coEvery
import io.mockk.mockk
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReminderToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: ReminderTools
    private val json = Json { ignoreUnknownKeys = true }

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

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(2, parsed.size)
        val first = parsed[0].jsonObject
        assertEquals(57872, first["id"]!!.jsonPrimitive.int)
        assertEquals("Reminder description 57872", first["description"]!!.jsonPrimitive.content)
        assertEquals("month", first["periodUnit"]!!.jsonPrimitive.content)
        assertEquals("2026-05-03", first["nextExecution"]!!.jsonPrimitive.content)
        assertEquals(7, first["numDaysForDueDate"]!!.jsonPrimitive.int)
        assertEquals(19297, first["tags"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.int)
        assertEquals("expense", first["type"]!!.jsonPrimitive.content)
        assertEquals(3, first["transactionType"]!!.jsonPrimitive.int)
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        coEvery { mockClient.getReminders() } throws BuxferApiException("boom")

        val result = tools.listReminders()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
