package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class GroupTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(GroupTools::class.java)

    suspend fun listGroups(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_groups")
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getGroups()))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_groups failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
