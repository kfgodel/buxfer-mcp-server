package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString

class LoanTools(private val client: BuxferClient) {

    suspend fun listLoans(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getLoans()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
