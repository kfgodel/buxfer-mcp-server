package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

class AccountTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(AccountTools::class.java)

    suspend fun listAccounts(): CallToolResult = runCatching {
        log.info("tool=buxfer_list_accounts")
        val accounts = client.getAccounts()
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(accounts))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_accounts failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
