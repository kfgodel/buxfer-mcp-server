package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferClient
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Entry point for the Buxfer MCP server.
 *
 * Startup ordering matters here. Logback resolves `${BUXFER_LOG_DIR}` and
 * `${BUXFER_LOG_LEVEL}` from `logback.xml` the first time any code calls
 * `LoggerFactory.getLogger(...)`. We must therefore run [Env.load] (which
 * promotes `.env` entries into JVM system properties) *before* acquiring
 * a logger, otherwise the log directory and level fall back to defaults
 * regardless of `.env` contents. Logger acquisition is intentionally
 * deferred to inside [main] for that reason — no file-level `val log`.
 *
 * Optional CLI arg:
 *   --env-file=<path>   Path to the `.env` file. Defaults to `./.env`.
 */
fun main(args: Array<String>) {
    // Suppress kotlin-logging's "kotlin-logging: initializing..." startup
    // println BEFORE any code triggers `KotlinLogging.<clinit>` (which the
    // MCP SDK does via its top-level `KotlinLogging.logger { }` vals).
    // The library writes that message directly to System.out, which would
    // corrupt the very first JSON-RPC frame on MCP's stdio transport. The
    // flag is only honoured if read before KotlinLogging's static init runs,
    // hence it is set here as the first executable statement in main().
    // See io.github.oshai.kotlinlogging.KotlinLogging:25-30.
    KotlinLoggingConfiguration.logStartupMessage = false

    // Load .env BEFORE first SLF4J call so Logback's property substitution
    // sees BUXFER_LOG_DIR / BUXFER_LOG_LEVEL when it initializes.
    val config = try {
        Env.load(args)
    } catch (e: IllegalStateException) {
        // No logger yet — go straight to stderr + exit. We deliberately do
        // not call fatal() here because doing so would acquire a logger
        // (triggering Logback init) before we know whether BUXFER_LOG_DIR
        // is even pointing somewhere writable.
        System.err.println("Startup failed: ${e.message}")
        exitProcess(1)
    }

    val log = LoggerFactory.getLogger("com.buxfer.mcp.Main")
    log.info("Buxfer MCP server starting (config from {})", config.sourcePath)

    try {
        runBlocking {
            BuxferClient().use { client ->
                client.login(config.email, config.password)
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
        fatal(log, "Fatal: ${e.message}", e)
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
private fun fatal(log: Logger, message: String, cause: Throwable? = null): Nothing {
    System.err.println(message)
    if (cause != null) log.error(message, cause) else log.error(message)
    exitProcess(1)
}
