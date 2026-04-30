package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class ReminderTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(ReminderTools::class.java)

    suspend fun listReminders(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_reminders")
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getReminders()))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_reminders failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
