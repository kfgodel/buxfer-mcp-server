package com.buxfer.mcp.api

import com.buxfer.mcp.api.models.Account
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.Budget
import com.buxfer.mcp.api.models.Contact
import com.buxfer.mcp.api.models.Group
import com.buxfer.mcp.api.models.Loan
import com.buxfer.mcp.api.models.Reminder
import com.buxfer.mcp.api.models.Tag
import com.buxfer.mcp.api.models.Transaction
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.api.models.TransactionsResult
import com.buxfer.mcp.api.models.UploadStatementResult
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val buxferJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class BuxferClient(engine: HttpClientEngine = CIO.create()) {

    companion object {
        private const val BASE_URL = "https://www.buxfer.com/api"
    }

    private val httpClient = HttpClient(engine)

    @Volatile internal var token: String? = null

    private fun requireToken() = token ?: throw BuxferApiException("Not logged in — call login() first")

    private suspend fun text(response: HttpResponse): String {
        if (!response.status.isSuccess()) throw BuxferApiException("HTTP ${response.status.value}: ${response.status.description}")
        return response.bodyAsText()
    }

    private fun responseBody(bodyText: String): JsonObject {
        val envelope = buxferJson.parseToJsonElement(bodyText).jsonObject
        val response = envelope["response"]?.jsonObject
            ?: throw BuxferApiException("Missing 'response' field in API reply")
        val status = response["status"]?.jsonPrimitive?.contentOrNull
        if (status == "error") {
            val error = response["error"]?.jsonPrimitive?.contentOrNull ?: status
            throw BuxferApiException(error)
        }
        return response
    }

    suspend fun login(email: String, password: String) {
        val response = httpClient.post("$BASE_URL/login") {
            setBody(FormDataContent(Parameters.build {
                append("email", email)
                append("password", password)
            }))
        }
        val body = responseBody(text(response))
        token = body["token"]?.jsonPrimitive?.contentOrNull
            ?: throw BuxferApiException("Login succeeded but no token in response")
    }

    suspend fun getAccounts(): List<Account> {
        val response = httpClient.get("$BASE_URL/accounts") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["accounts"] ?: JsonArray(emptyList()))
    }

    suspend fun getTransactions(filters: TransactionFilters = TransactionFilters()): TransactionsResult {
        val response = httpClient.get("$BASE_URL/transactions") {
            parameter("token", requireToken())
            filters.accountId?.let { parameter("accountId", it) }
            filters.accountName?.let { parameter("accountName", it) }
            filters.tagId?.let { parameter("tagId", it) }
            filters.tagName?.let { parameter("tagName", it) }
            filters.startDate?.let { parameter("startDate", it) }
            filters.endDate?.let { parameter("endDate", it) }
            filters.budgetId?.let { parameter("budgetId", it) }
            filters.budgetName?.let { parameter("budgetName", it) }
            filters.contactId?.let { parameter("contactId", it) }
            filters.contactName?.let { parameter("contactName", it) }
            filters.groupId?.let { parameter("groupId", it) }
            filters.groupName?.let { parameter("groupName", it) }
            filters.status?.let { parameter("status", it) }
            filters.page?.let { parameter("page", it) }
        }
        val body = responseBody(text(response))
        val transactions: List<Transaction> = buxferJson.decodeFromJsonElement(
            body["transactions"] ?: JsonArray(emptyList())
        )
        val numTransactions = body["numTransactions"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        return TransactionsResult(transactions, numTransactions)
    }

    suspend fun addTransaction(params: AddTransactionParams): Transaction {
        val response = httpClient.post("$BASE_URL/transaction_add") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("description", params.description)
                append("amount", params.amount.toString())
                append("accountId", params.accountId.toString())
                append("date", params.date)
                params.tags?.let { append("tags", it) }
                params.type?.let { append("type", it) }
                params.status?.let { append("status", it) }
            }))
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(JsonObject(body))
    }

    suspend fun editTransaction(id: Int, params: AddTransactionParams): Transaction {
        val response = httpClient.post("$BASE_URL/transaction_edit") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("id", id.toString())
                append("description", params.description)
                append("amount", params.amount.toString())
                append("accountId", params.accountId.toString())
                append("date", params.date)
                params.tags?.let { append("tags", it) }
                params.type?.let { append("type", it) }
                params.status?.let { append("status", it) }
            }))
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(JsonObject(body))
    }

    suspend fun deleteTransaction(id: Int) {
        val response = httpClient.post("$BASE_URL/transaction_delete") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("id", id.toString())
            }))
        }
        responseBody(text(response))
    }

    suspend fun uploadStatement(accountId: Int, statement: String, dateFormat: String? = null): UploadStatementResult {
        val response = httpClient.post("$BASE_URL/upload_statement") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("accountId", accountId.toString())
                append("statement", statement)
                dateFormat?.let { append("dateFormat", it) }
            }))
        }
        val body = responseBody(text(response))
        val uploaded = body["uploaded"]?.jsonPrimitive?.intOrNull ?: 0
        val balance = body["balance"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        return UploadStatementResult(uploaded, balance)
    }

    suspend fun getTags(): List<Tag> {
        val response = httpClient.get("$BASE_URL/tags") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["tags"] ?: JsonArray(emptyList()))
    }

    suspend fun getBudgets(): List<Budget> {
        val response = httpClient.get("$BASE_URL/budgets") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["budgets"] ?: JsonArray(emptyList()))
    }

    suspend fun getReminders(): List<Reminder> {
        val response = httpClient.get("$BASE_URL/reminders") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["reminders"] ?: JsonArray(emptyList()))
    }

    suspend fun getGroups(): List<Group> {
        val response = httpClient.get("$BASE_URL/groups") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["groups"] ?: JsonArray(emptyList()))
    }

    suspend fun getContacts(): List<Contact> {
        val response = httpClient.get("$BASE_URL/contacts") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["contacts"] ?: JsonArray(emptyList()))
    }

    suspend fun getLoans(): List<Loan> {
        val response = httpClient.get("$BASE_URL/loans") {
            parameter("token", requireToken())
        }
        val body = responseBody(text(response))
        return buxferJson.decodeFromJsonElement(body["loans"] ?: JsonArray(emptyList()))
    }
}
