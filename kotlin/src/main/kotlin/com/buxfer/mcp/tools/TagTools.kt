package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class TagTools(private val client: BuxferClient) {

    companion object {
        private val log = LoggerFactory.getLogger(TagTools::class.java)
    }

    suspend fun listTags(): CallToolResult =
        mcpTool("buxfer_list_tags", log) { client.getTags() }
}
