package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString

class GroupTools(private val client: BuxferClient) {

    suspend fun listGroups(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getGroups()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
