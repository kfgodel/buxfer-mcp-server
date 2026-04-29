package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString

class TagTools(private val client: BuxferClient) {

    suspend fun listTags(): CallToolResult = runCatching {
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getTags()))))
    }.getOrElse { e ->
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
