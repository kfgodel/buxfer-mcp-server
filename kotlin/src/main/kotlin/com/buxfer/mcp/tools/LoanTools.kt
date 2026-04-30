package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class LoanTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(LoanTools::class.java)

    suspend fun listLoans(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_loans")
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(client.getLoans()))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_loans failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
