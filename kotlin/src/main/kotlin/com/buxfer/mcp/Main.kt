package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.buxfer.mcp.Main")

fun main() {
    // Mirror Logback's ${BUXFER_LOG_DIR:-./logs} resolution so the operator sees the same path Logback writes to.
    val logDir = System.getenv("BUXFER_LOG_DIR")?.takeIf { it.isNotBlank() } ?: "./logs"
    log.info("Buxfer MCP server starting (log dir={})", logDir)

    val email = System.getenv("BUXFER_EMAIL")
    val password = System.getenv("BUXFER_PASSWORD")
    if (email.isNullOrBlank() || password.isNullOrBlank()) {
        // stderr in addition to log: Logback may not have flushed if the process exits this fast,
        // and the operator launching the process needs to see why it died in their terminal.
        // (BUXFER_EMAIL is PII per the redaction policy in logback.xml — never log its value.)
        val msg = "BUXFER_EMAIL and BUXFER_PASSWORD must be set"
        System.err.println(msg)
        log.error(msg)
        exitProcess(1)
    }

    runBlocking {
        val client = BuxferClient()
        try {
            client.login(email, password)
        } catch (e: BuxferApiException) {
            val msg = "Login failed: ${e.message}"
            System.err.println(msg)
            log.error(msg, e)
            exitProcess(1)
        }
        log.info("Login OK; starting MCP server")
        BuxferMcpServer(client).start()
    }
}
