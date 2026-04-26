package com.buxfer.mcp

// TODO: Start the MCP server over stdio transport.
//
// Steps:
//   1. Read BUXFER_EMAIL and BUXFER_PASSWORD from environment variables.
//   2. Instantiate BuxferClient and call login() to obtain a session token.
//   3. Instantiate BuxferMcpServer with the authenticated client.
//   4. Call BuxferMcpServer.start() which blocks on stdin until the client disconnects.
//
// Error handling:
//   - If credentials are missing, print to stderr and exit with code 1.
//   - If login fails, print the error to stderr and exit with code 1.
//   - Never write anything to stdout before starting the MCP server (breaks stdio transport).

fun main() {
    TODO("Not yet implemented")
}
