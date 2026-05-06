package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import org.slf4j.LoggerFactory

class BudgetTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(BudgetTools::class.java)

    suspend fun listBudgets(): CallToolResult =
        mcpTool("buxfer_list_budgets", log) { client.getBudgets() }
}
