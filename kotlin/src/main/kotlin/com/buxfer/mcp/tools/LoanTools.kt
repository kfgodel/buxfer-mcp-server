package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class LoanTools(private val client: BuxferClient) {

    companion object {
        private val log = LoggerFactory.getLogger(LoanTools::class.java)
    }

    suspend fun listLoans(): CallToolResult =
        mcpTool("buxfer_list_loans", log) { client.getLoans() }
}
