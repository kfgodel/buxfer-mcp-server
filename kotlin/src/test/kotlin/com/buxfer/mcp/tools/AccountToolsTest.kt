package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO: Implement all tests once AccountTools is implemented.
//
// Same test strategy as TransactionToolsTest: MockK for BuxferClient,
// verify JSON output, verify error propagation.

class AccountToolsTest {

    private val mockClient = mockk<BuxferClient>()
    private lateinit var tools: AccountTools

    @BeforeEach
    fun setUp() {
        tools = AccountTools(mockClient)
    }

    @Test
    fun `listAccounts returns JSON array of accounts`() = runTest {
        // TODO: coEvery { mockClient.getAccounts() } returns <list from accounts fixture>
        //       Call tools handler for buxfer_list_accounts
        //       Assert JSON contains 3 accounts, first id == 1001
        TODO("Not yet implemented")
    }

    @Test
    fun `tool returns error result when client throws`() = runTest {
        // TODO: coEvery { mockClient.getAccounts() } throws BuxferApiException("error")
        //       Assert isError = true in the tool result
        TODO("Not yet implemented")
    }
}
