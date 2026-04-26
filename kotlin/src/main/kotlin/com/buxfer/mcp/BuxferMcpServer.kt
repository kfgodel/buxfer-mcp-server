package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.tools.AccountTools
import com.buxfer.mcp.tools.LookupTools
import com.buxfer.mcp.tools.TransactionTools

// TODO: Bootstrap the MCP server and register all tools.
//
// Responsibilities:
//   - Create an MCP Server instance using the kotlin-sdk (io.modelcontextprotocol:kotlin-sdk).
//   - Set server metadata: name = "buxfer", version = "1.0.0".
//   - Register tools from TransactionTools, AccountTools, and LookupTools.
//   - Expose a start() suspend function that starts stdio transport and suspends until disconnect.
//
// Tool registration pattern (per MCP Kotlin SDK):
//   server.addTool(name, description, inputSchema) { request -> ... }
//
// Delegate each tool handler to the corresponding *Tools class method.
// See ../shared/api-spec/buxfer-api.md for the full list of tools and their parameter contracts.

class BuxferMcpServer(private val client: BuxferClient) {

    private val transactionTools = TransactionTools(client)
    private val accountTools = AccountTools(client)
    private val lookupTools = LookupTools(client)

    suspend fun start() {
        TODO("Not yet implemented")
    }
}
