package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class TagTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(TagTools::class.java)

    suspend fun listTags(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_tags")
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getTags()))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_tags failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
