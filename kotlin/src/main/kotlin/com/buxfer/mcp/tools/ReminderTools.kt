package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class ReminderTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(ReminderTools::class.java)

    suspend fun listReminders(): CallToolResult =
        mcpTool("buxfer_list_reminders", log) { client.getReminders() }
}
