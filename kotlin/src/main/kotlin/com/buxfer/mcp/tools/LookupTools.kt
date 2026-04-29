package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString

class LookupTools(private val client: BuxferClient) {

    suspend fun listTags(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getTags()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun listBudgets(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getBudgets()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun listReminders(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getReminders()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun listGroups(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getGroups()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun listContacts(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getContacts()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun listLoans(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getLoans()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
