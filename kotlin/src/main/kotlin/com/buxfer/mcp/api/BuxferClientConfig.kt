package com.buxfer.mcp.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

/**
 * Configuration for [BuxferClient]. Holds plain values — env-var resolution
 * (from `.env` or the OS environment) lives entirely in
 * [com.buxfer.mcp.Env], which produces a [com.buxfer.mcp.BuxferMcpConfig]
 * that callers thread through to construct this config. Keeping all
 * env / properties handling in one place avoids two layers of fallback
 * logic disagreeing.
 *
 * Not a data class: [engine] is a resource — copy() semantics and structural equality
 * on a live HTTP engine are not meaningful.
 *
 * Timeouts default to values appropriate for a finance API on the public internet.
 * They cap how long a tool call can hang on a slow or wedged Buxfer endpoint —
 * without them, an MCP tool invocation could block indefinitely.
 */
class BuxferClientConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val engine: HttpClientEngine = CIO.create(),
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MS,
) {
    companion object {
        /** Default Buxfer REST API base URL. Override with BUXFER_API_BASE_URL env var. */
        const val DEFAULT_BASE_URL = "https://www.buxfer.com/api"

        /** TCP connect to buxfer.com — generous for a public-internet hop. */
        const val DEFAULT_CONNECT_TIMEOUT_MS: Long = 10_000

        /** Overall cap on a single request, including reading the response body. */
        const val DEFAULT_REQUEST_TIMEOUT_MS: Long = 30_000

        /** Inactivity timeout between bytes once the connection is open. */
        const val DEFAULT_SOCKET_TIMEOUT_MS: Long = 30_000
    }
}
