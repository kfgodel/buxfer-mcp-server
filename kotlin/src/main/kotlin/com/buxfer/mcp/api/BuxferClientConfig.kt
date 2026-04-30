package com.buxfer.mcp.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

/**
 * Configuration for [BuxferClient]. Reads from environment variables when constructed
 * with defaults, so callers ([Main][com.buxfer.mcp.Main], tests) do not need to
 * inspect env vars themselves.
 *
 * Not a data class: [engine] is a resource — copy() semantics and structural equality
 * on a live HTTP engine are not meaningful.
 */
class BuxferClientConfig(
    val baseUrl: String = System.getenv("BUXFER_API_BASE_URL")
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_BASE_URL,
    val engine: HttpClientEngine = CIO.create(),
) {
    companion object {
        /** Default Buxfer REST API base URL. Override with BUXFER_API_BASE_URL env var. */
        const val DEFAULT_BASE_URL = "https://www.buxfer.com/api"
    }
}
