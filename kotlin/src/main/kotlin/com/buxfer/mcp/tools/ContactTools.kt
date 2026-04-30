package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class ContactTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(ContactTools::class.java)

    suspend fun listContacts(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_contacts")
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getContacts()))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_contacts failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
