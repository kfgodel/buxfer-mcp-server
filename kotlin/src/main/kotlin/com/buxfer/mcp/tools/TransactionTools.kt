package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.api.models.UploadStatementResult
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class TransactionTools(private val client: BuxferClient) {

    suspend fun listTransactions(args: JsonObject?): CallToolResult = runCatching {
        val filters = TransactionFilters(
            accountId = args?.get("accountId")?.jsonPrimitive?.intOrNull,
            accountName = args?.get("accountName")?.jsonPrimitive?.contentOrNull,
            tagId = args?.get("tagId")?.jsonPrimitive?.intOrNull,
            tagName = args?.get("tagName")?.jsonPrimitive?.contentOrNull,
            startDate = args?.get("startDate")?.jsonPrimitive?.contentOrNull,
            endDate = args?.get("endDate")?.jsonPrimitive?.contentOrNull,
            budgetId = args?.get("budgetId")?.jsonPrimitive?.intOrNull,
            budgetName = args?.get("budgetName")?.jsonPrimitive?.contentOrNull,
            contactId = args?.get("contactId")?.jsonPrimitive?.intOrNull,
            contactName = args?.get("contactName")?.jsonPrimitive?.contentOrNull,
            groupId = args?.get("groupId")?.jsonPrimitive?.intOrNull,
            groupName = args?.get("groupName")?.jsonPrimitive?.contentOrNull,
            status = args?.get("status")?.jsonPrimitive?.contentOrNull,
            page = args?.get("page")?.jsonPrimitive?.intOrNull
        )
        val result = client.getTransactions(filters)
        val json = buildString {
            append("{\"transactions\":")
            append(buxferJson.encodeToString(result.transactions))
            append(",\"numTransactions\":")
            append(result.numTransactions)
            append("}")
        }
        CallToolResult(content = listOf(TextContent(json)))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun addTransaction(args: JsonObject?): CallToolResult = runCatching {
        val params = AddTransactionParams(
            description = args?.get("description")?.jsonPrimitive?.contentOrNull ?: "",
            amount = args?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            accountId = args?.get("accountId")?.jsonPrimitive?.intOrNull ?: 0,
            date = args?.get("date")?.jsonPrimitive?.contentOrNull ?: "",
            tags = args?.get("tags")?.jsonPrimitive?.contentOrNull,
            type = args?.get("type")?.jsonPrimitive?.contentOrNull,
            status = args?.get("status")?.jsonPrimitive?.contentOrNull
        )
        val tx = client.addTransaction(params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun editTransaction(args: JsonObject?): CallToolResult = runCatching {
        val id = args?.get("id")?.jsonPrimitive?.intOrNull ?: 0
        val params = AddTransactionParams(
            description = args?.get("description")?.jsonPrimitive?.contentOrNull ?: "",
            amount = args?.get("amount")?.jsonPrimitive?.doubleOrNull ?: 0.0,
            accountId = args?.get("accountId")?.jsonPrimitive?.intOrNull ?: 0,
            date = args?.get("date")?.jsonPrimitive?.contentOrNull ?: "",
            tags = args?.get("tags")?.jsonPrimitive?.contentOrNull,
            type = args?.get("type")?.jsonPrimitive?.contentOrNull,
            status = args?.get("status")?.jsonPrimitive?.contentOrNull
        )
        val tx = client.editTransaction(id, params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun deleteTransaction(args: JsonObject?): CallToolResult = runCatching {
        val id = args?.get("id")?.jsonPrimitive?.intOrNull ?: 0
        client.deleteTransaction(id)
        CallToolResult(content = listOf(TextContent("{\"deleted\":true,\"id\":$id}")))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun uploadStatement(args: JsonObject?): CallToolResult = runCatching {
        val accountId = args?.get("accountId")?.jsonPrimitive?.intOrNull ?: 0
        val statement = args?.get("statement")?.jsonPrimitive?.contentOrNull ?: ""
        val dateFormat = args?.get("dateFormat")?.jsonPrimitive?.contentOrNull
        val result = client.uploadStatement(accountId, statement, dateFormat)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(result))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
