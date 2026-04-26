package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO: Implement all tests once TransactionTools is implemented.
//
// Test strategy:
//   - Use MockK to mock BuxferClient (no HTTP involved at this layer).
//   - Verify that each tool method calls the correct BuxferClient method
//     with the correct parameters.
//   - Verify that the JSON string returned by each tool correctly represents
//     the model object(s) returned by BuxferClient.
//   - Verify that BuxferClient exceptions are surfaced as MCP error results
//     (isError = true) rather than propagating as Kotlin exceptions.
//
// Each test should cover:
//   1. Happy path — correct input, expected output JSON
//   2. Error path — BuxferClient throws, tool returns error result

class TransactionToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: TransactionTools

    @BeforeEach
    fun setUp() {
        tools = TransactionTools(mockClient)
    }

    @Test
    fun `listTransactions returns JSON array of transactions`() = runTest {
        // TODO: coEvery { mockClient.getTransactions(any()) } returns <parsed fixture>
        //       Call tools handler for buxfer_list_transactions
        //       Assert returned JSON contains expected transaction fields
        TODO("Not yet implemented")
    }

    @Test
    fun `listTransactions with filters passes them to client`() = runTest {
        // TODO: Provide accountId, startDate, endDate in the tool input
        //       Assert mockClient.getTransactions was called with those filters
        TODO("Not yet implemented")
    }

    @Test
    fun `addTransaction calls client with correct params`() = runTest {
        // TODO: coEvery { mockClient.addTransaction(any()) } returns <parsed fixture>
        //       Assert tool returns JSON of the created transaction
        TODO("Not yet implemented")
    }

    @Test
    fun `editTransaction passes id and fields to client`() = runTest {
        // TODO: coEvery { mockClient.editTransaction(5001, any()) } returns <updated fixture>
        //       Assert tool returns JSON of the updated transaction
        TODO("Not yet implemented")
    }

    @Test
    fun `deleteTransaction returns confirmation on success`() = runTest {
        // TODO: coEvery { mockClient.deleteTransaction(5001) } returns Unit
        //       Assert tool result contains "deleted" or "success" message
        TODO("Not yet implemented")
    }

    @Test
    fun `uploadStatement returns uploaded count and balance`() = runTest {
        // TODO: coEvery { mockClient.uploadStatement(...) } returns <upload fixture>
        //       Assert tool result JSON contains uploaded == 5
        TODO("Not yet implemented")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        // TODO: coEvery { mockClient.getTransactions(any()) } throws BuxferApiException("Invalid token")
        //       Assert the tool result has isError = true and the message in content
        TODO("Not yet implemented")
    }
}
