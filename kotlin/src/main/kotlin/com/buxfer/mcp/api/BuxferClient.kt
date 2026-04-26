package com.buxfer.mcp.api

import com.buxfer.mcp.api.models.*

// TODO: Implement a Ktor-based HTTP client for the Buxfer REST API.
//
// IMPORTANT — testability: accept an optional HttpClientEngine parameter so
// tests can inject a MockEngine without making real network calls:
//   class BuxferClient(engine: HttpClientEngine = CIO.create())
//
// Base URL: https://www.buxfer.com/api
//
// Responsibilities:
//   - login(email, password): POST /api/login, store token in a @Volatile private field.
//   - All other methods append ?token=<stored token> to every request.
//   - Deserialize responses with kotlinx.serialization.
//   - Throw a BuxferApiException (define it here or in a separate file) for non-OK statuses.
//
// HTTP client setup:
//   - Use HttpClient(CIO) with ContentNegotiation + kotlinx.serialization JSON plugin.
//   - All methods are suspend functions.
//
// Methods to implement (one per Buxfer API endpoint):
//   suspend fun login(email: String, password: String)
//   suspend fun getTransactions(filters: TransactionFilters): TransactionsResponse
//   suspend fun addTransaction(params: AddTransactionParams): Transaction
//   suspend fun editTransaction(id: Int, params: AddTransactionParams): Transaction
//   suspend fun deleteTransaction(id: Int)
//   suspend fun uploadStatement(accountId: Int, statement: String, dateFormat: String?): UploadStatementResponse
//   suspend fun getAccounts(): List<Account>
//   suspend fun getLoans(): List<Loan>
//   suspend fun getTags(): List<Tag>
//   suspend fun getBudgets(): List<Budget>
//   suspend fun getReminders(): List<Reminder>
//   suspend fun getGroups(): List<Group>
//   suspend fun getContacts(): List<Contact>
//
// See ../../../../../../shared/api-spec/buxfer-api.md for full parameter and response contracts.

class BuxferClient {
    // TODO: Initialize Ktor HttpClient here

    @Volatile
    private var token: String? = null

    suspend fun login(email: String, password: String) {
        TODO("Not yet implemented")
    }
}
