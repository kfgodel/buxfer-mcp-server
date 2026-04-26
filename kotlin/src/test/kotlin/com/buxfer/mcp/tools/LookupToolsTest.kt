package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO: Implement all tests once LookupTools is implemented.
//
// Same test strategy as TransactionToolsTest and AccountToolsTest.
// One test per tool (happy path + error path).

class LookupToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: LookupTools

    @BeforeEach
    fun setUp() {
        tools = LookupTools(mockClient)
    }

    @Test
    fun `listTags returns JSON array with parentId preserved`() = runTest {
        // TODO: coEvery { mockClient.getTags() } returns <list from tags fixture>
        //       Assert 6 tags, second has parentId == 3001
        TODO("Not yet implemented")
    }

    @Test
    fun `listBudgets returns JSON with tags and keywords arrays`() = runTest {
        // TODO: coEvery { mockClient.getBudgets() } returns <list from budgets fixture>
        //       Assert first budget remaining == 480.57
        TODO("Not yet implemented")
    }

    @Test
    fun `listReminders returns JSON array of reminders`() = runTest {
        // TODO: coEvery { mockClient.getReminders() } returns <list from reminders fixture>
        //       Assert first reminder name == "Rent Payment"
        TODO("Not yet implemented")
    }

    @Test
    fun `listGroups returns JSON with nested members`() = runTest {
        // TODO: coEvery { mockClient.getGroups() } returns <list from groups fixture>
        //       Assert group "Roommates" has 3 members
        TODO("Not yet implemented")
    }

    @Test
    fun `listContacts returns JSON array of contacts`() = runTest {
        // TODO: coEvery { mockClient.getContacts() } returns <list from contacts fixture>
        //       Assert first contact email == "contact.one@example.com"
        TODO("Not yet implemented")
    }

    @Test
    fun `listLoans returns JSON array of loans`() = runTest {
        // TODO: coEvery { mockClient.getLoans() } returns <list from loans fixture>
        //       Assert first loan type == "contact"
        TODO("Not yet implemented")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        // TODO: Any mockClient method throws BuxferApiException
        //       Assert isError = true in the tool result
        TODO("Not yet implemented")
    }
}
