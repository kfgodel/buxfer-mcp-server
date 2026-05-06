package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class GroupTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(GroupTools::class.java)

    suspend fun listGroups(): CallToolResult =
        mcpTool("buxfer_list_groups", log) { client.getGroups() }
}
