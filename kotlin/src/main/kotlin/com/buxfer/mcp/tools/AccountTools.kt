package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class AccountTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(AccountTools::class.java)

    suspend fun listAccounts(): CallToolResult =
        mcpTool("buxfer_list_accounts", log) { client.getAccounts() }
}
