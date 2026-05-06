package com.buxfer.mcp.tools

import com.buxfer.mcp.api.buxferJson
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.encodeToString
import org.slf4j.Logger

/**
 * Runs an MCP tool body and packages the result as a [CallToolResult]:
 * - logs entry at INFO with the tool's `name`
 * - serializes the block's return value to JSON via [buxferJson]
 * - on any exception, logs at ERROR and returns a result with `isError = true`
 *
 * `inline reified T` is required so each call site keeps a concrete type for
 * `encodeToString` — a non-reified helper would fall back to `Any`'s serializer
 * and fail at runtime. Each tool class still owns its own [Logger] so the log
 * line carries the tool class as its source.
 */
internal suspend inline fun <reified T> mcpTool(
    name: String,
    log: Logger,
    block: suspend () -> T,
): CallToolResult = runCatching {
    log.info("tool={}", name)
    val result = block()
    CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(result))))
}.getOrElse { e ->
    log.error("tool={} failed", name, e)
    CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
}
