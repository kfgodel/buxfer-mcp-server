package com.buxfer.mcp.testing

import com.buxfer.mcp.TestFixtureLoader
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Test-only Ktor MockEngine that serves the captured Buxfer fixtures from
 * `shared/test-fixtures/wiremock/__files/`. Mirrors the response contract
 * of the embedded WireMock used at the integration tier, so unit-level and
 * integration-level tests share the same expected response shapes.
 *
 * Pass [overrides] (path-suffix → response-body) to replace any default response,
 * typically to inject error envelopes or schema-drift payloads in negative tests.
 */
object MockEngineSupport {

    /**
     * The /upload_statement endpoint's default body. Lives here as a constant so the
     * integration test (which stubs WireMock instead of MockEngine) can inject the
     * same body without duplication. Should be removed once a real upload_statement
     * fixture is captured — see api-recordings/CLAUDE.md.
     */
    const val UPLOAD_STATEMENT_OK_BODY = """{"response":{"status":"OK","uploaded":15,"balance":1234.56}}"""

    // Endpoints whose default response is the fixture file matching the path suffix.
    private val FIXTURE_ENDPOINTS = listOf(
        "/login", "/accounts", "/transactions",
        "/transaction_add", "/transaction_edit", "/transaction_delete",
        "/tags", "/budgets", "/reminders", "/groups", "/contacts", "/loans"
    )

    // Endpoints whose default response is hand-written (no fixture file or fixture is empty).
    private val INLINE_BODIES = mapOf(
        "/upload_statement" to UPLOAD_STATEMENT_OK_BODY
    )

    fun newEngine(overrides: Map<String, String> = emptyMap()): MockEngine = MockEngine { request ->
        val path = request.url.encodedPath
        val body = overrides.entries.firstOrNull { path.endsWith(it.key) }?.value
            ?: FIXTURE_ENDPOINTS.firstOrNull { path.endsWith(it) }?.let { TestFixtureLoader.load(it.removePrefix("/")) }
            ?: INLINE_BODIES.entries.firstOrNull { path.endsWith(it.key) }?.value
            ?: """{"response":{"status":"error","error":"Not found"}}"""
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
}
