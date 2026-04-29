package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.models.Budget
import com.buxfer.mcp.api.models.Contact
import com.buxfer.mcp.api.models.Group
import com.buxfer.mcp.api.models.Loan
import com.buxfer.mcp.api.models.Reminder
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

class LookupToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: LookupTools
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        tools = LookupTools(mockClient)
    }

    @Test
    fun `listTags returns JSON array with parentId preserved`() = runTest {
        val tags = listOf(
            Tag(id = 9125, name = "Tag 9125", relativeName = "tag 9125", parentId = null),
            Tag(id = 58045, name = "Tag 58045", relativeName = "tag 58045", parentId = null),
            Tag(id = 57902, name = "Tag 57902", relativeName = "tag 57902", parentId = 58046)
        )
        coEvery { mockClient.getTags() } returns tags

        val result = tools.listTags()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(3, parsed.size)
        assertEquals(58046, parsed[2].jsonObject["parentId"]!!.jsonPrimitive.int)
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
    fun `listGroups returns empty JSON array from fixture`() = runTest {
        coEvery { mockClient.getGroups() } returns emptyList()

        val result = tools.listGroups()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(0, parsed.size)
    }

    @Test
    fun `listContacts returns JSON array of contacts`() = runTest {
        val contacts = listOf(
            Contact(id = 4436, name = "Contact 1", email = "contact.1@example.com", balance = 0.0),
            Contact(id = 8953, name = "Contact 2", email = "contact.2@example.com", balance = 5000.0),
            Contact(id = 11481, name = "Contact 3", email = "contact.3@example.com", balance = 0.0),
            Contact(id = 31608, name = "Contact 4", email = "contact.4@example.com", balance = 0.0)
        )
        coEvery { mockClient.getContacts() } returns contacts

        val result = tools.listContacts()

        val parsed = json.parseToJsonElement((result.content[0] as TextContent).text).jsonArray
        assertEquals(4, parsed.size)
        assertEquals("contact.1@example.com", parsed[0].jsonObject["email"]!!.jsonPrimitive.content)
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
        coEvery { mockClient.getTags() } throws BuxferApiException("boom")

        val result = tools.listTags()

        assertTrue(result.isError == true)
        assertTrue((result.content[0] as TextContent).text.contains("Error: boom"))
    }
}
