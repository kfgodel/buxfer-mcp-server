package com.buxfer.mcp

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
        // BUXFER_EMAIL is PII per the redaction policy in logback.xml — never log its value.
        fatal("BUXFER_EMAIL and BUXFER_PASSWORD must be set")
    }

    try {
        runBlocking {
            BuxferClient().use { client ->
                client.login(email, password)
                log.info("Login OK; starting MCP server")
                BuxferMcpServer(client).start()
            }
        }
    } catch (e: Exception) {
        // Defense-in-depth: this is the last line for any fatal error — login failure
        // (BuxferApiException, IOException, timeout), server init failure, transport
        // errors mid-session. Main never relies on downstream layers wrapping things;
        // whatever escapes runBlocking gets logged AND surfaced on stderr before we
        // exit non-zero.
        fatal("Fatal: ${e.message}", e)
    }
}

/**
 * Surface a fatal startup/runtime error and terminate.
 *
 * Writes to stderr in addition to the log because Logback may not have flushed
 * the file appender by the time we exit, and the operator launching the process
 * needs to see why it died in their terminal.
 *
 * Returns [Nothing] so smart-casts survive the call site (e.g. nullable env-var
 * checks remain non-null in code that follows a guarded `fatal(...)`).
 */
private fun fatal(message: String, cause: Throwable? = null): Nothing {
    System.err.println(message)
    if (cause != null) log.error(message, cause) else log.error(message)
    exitProcess(1)
}
