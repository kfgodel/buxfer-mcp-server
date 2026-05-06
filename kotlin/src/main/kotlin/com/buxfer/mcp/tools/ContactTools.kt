package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class ContactTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(ContactTools::class.java)

    suspend fun listContacts(): CallToolResult =
        mcpTool("buxfer_list_contacts", log) { client.getContacts() }
}
