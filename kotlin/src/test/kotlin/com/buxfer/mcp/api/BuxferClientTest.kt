package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// TODO: Implement all tests once BuxferClient is implemented.
//
// BuxferClient must accept an optional HttpClientEngine parameter so tests can
// inject a MockEngine without making real network calls:
//   class BuxferClient(engine: HttpClientEngine = CIO.create()) { ... }
//
// Test strategy:
//   - Each test creates a MockEngine that returns the relevant fixture JSON.
//   - Assertions verify that BuxferClient correctly deserializes the response
//     into the expected model types.
//   - A separate test verifies that HTTP errors / Buxfer error responses
//     are translated into BuxferApiException.
//
// See shared/test-fixtures/responses/ for all fixture files.
// See shared/api-spec/buxfer-api.md for full response contracts.

class BuxferClientTest {

    private lateinit var client: BuxferClient

    @BeforeEach
    fun setUp() {
        // TODO: Construct BuxferClient with a MockEngine that dispatches
        //       fixture responses based on request.url.encodedPath.
        //       Pre-set client.token to a fake value ("test-token") so
        //       individual tests don't need to call login().
    }

    @Test
    fun `login stores token on success`() = runTest {
        // TODO: MockEngine returns {"status":"OK","token":"test-token"}
        //       Assert that calling login() stores the token internally.
        TODO("Not yet implemented")
    }

    @Test
    fun `getAccounts returns parsed Account list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("accounts")
        //       Call client.getAccounts() and assert:
        //         - list has 3 items
        //         - first account id == 1001, name == "Test Checking", balance == 2547.83
        TODO("Not yet implemented")
    }

    @Test
    fun `getTransactions returns parsed Transaction list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("transactions")
        //       Assert list size == 5, first transaction type == "expense"
        TODO("Not yet implemented")
    }

    @Test
    fun `addTransaction returns created Transaction`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("transaction_add")
        //       Call client.addTransaction(...) and assert returned id == 5099
        TODO("Not yet implemented")
    }

    @Test
    fun `editTransaction returns updated Transaction`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("transaction_edit")
        //       Assert returned description == "Updated Grocery Store"
        TODO("Not yet implemented")
    }

    @Test
    fun `deleteTransaction succeeds without exception`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("transaction_delete")
        //       Assert no exception is thrown
        TODO("Not yet implemented")
    }

    @Test
    fun `uploadStatement returns upload count and balance`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("upload_statement")
        //       Assert uploaded == 5, balance == 2547.83
        TODO("Not yet implemented")
    }

    @Test
    fun `getTags returns parsed Tag list with parentId`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("tags")
        //       Assert list size == 6, second tag parentId == 3001
        TODO("Not yet implemented")
    }

    @Test
    fun `getBudgets returns parsed Budget list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("budgets")
        //       Assert first budget remaining == 480.57
        TODO("Not yet implemented")
    }

    @Test
    fun `getReminders returns parsed Reminder list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("reminders")
        //       Assert first reminder name == "Rent Payment"
        TODO("Not yet implemented")
    }

    @Test
    fun `getGroups returns groups with nested members`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("groups")
        //       Assert first group name == "Roommates" and has 3 members
        TODO("Not yet implemented")
    }

    @Test
    fun `getContacts returns parsed Contact list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("contacts")
        //       Assert first contact email == "contact.one@example.com"
        TODO("Not yet implemented")
    }

    @Test
    fun `getLoans returns parsed Loan list`() = runTest {
        // TODO: MockEngine returns TestFixtureLoader.load("loans")
        //       Assert first loan type == "contact"
        TODO("Not yet implemented")
    }

    @Test
    fun `throws BuxferApiException on non-OK status`() = runTest {
        // TODO: MockEngine returns {"status":"error","error":"Invalid token"}
        //       Assert that any API call throws BuxferApiException with that message
        TODO("Not yet implemented")
    }

    @Test
    fun `throws on HTTP error response`() = runTest {
        // TODO: MockEngine returns HTTP 500
        //       Assert that any API call throws an appropriate exception
        TODO("Not yet implemented")
    }
}
