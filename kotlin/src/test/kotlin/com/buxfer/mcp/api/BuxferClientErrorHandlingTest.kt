package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.testing.MockEngineSupport
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BuxferClientErrorHandlingTest {

    @Test
    fun `throws BuxferApiException on non-OK status`() = runTest {
        BuxferClient(BuxferClientConfig(engine = MockEngineSupport.newEngine(overrides = mapOf(
            "/accounts" to """{"response":{"status":"error","error":"Invalid token"}}"""
        )))).use { errorClient ->
            errorClient.login("user@example.com", "password")
            val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }
            assertThat(ex.message).isEqualTo("Invalid token")
        }
    }

    @Test
    fun `parse error surfaces field, type, and endpoint context`() = runTest {
        // Budget.id is the identity field — required, no default. If the API drops it,
        // kotlinx.serialization throws SerializationException naming the missing field
        // and type, and BuxferClient.traced wraps it as BuxferApiException with method+path
        // context. Together that's enough to diagnose real API drift on sight.
        //
        // Note: this exercises the strict-decode-as-throw path used by all unmigrated
        // endpoints (the ones still going through `getList`). Migrated endpoints
        // (`getAccounts`, `getTags`) use the non-throwing `validateSchema` path — their
        // drift coverage lives in their respective `logs schema-drift warning` tests.
        // As more endpoints migrate, repoint this test to whichever still uses `getList`.
        val engine = MockEngineSupport.newEngine(overrides = mapOf(
            "/budgets" to """{"response":{"status":"OK","budgets":[{"name":"x"}]}}"""
        ))
        BuxferClient(BuxferClientConfig(engine = engine)).use { parseClient ->
            parseClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { parseClient.getBudgets() }

            assertThat(ex.message)
                .contains("GET /budgets")
                .contains("id")
                .contains("Budget")
            assertThat(ex.cause).isInstanceOf(kotlinx.serialization.SerializationException::class.java)
        }
    }

    @Test
    fun `throws on HTTP error response`() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/login"))
                respond(TestFixtureLoader.load("login"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                respond("Server Error", HttpStatusCode.InternalServerError)
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { errorClient ->
            errorClient.login("user@example.com", "password")
            assertThrows<BuxferApiException> { errorClient.getAccounts() }
        }
    }

    @Test
    fun `wraps IOException as BuxferApiException with method, path, and cause`() = runTest {
        // Simulates a network-level failure (server down, DNS fail, connection reset).
        // java.io.IOException is the superclass for ConnectException, UnknownHostException,
        // SocketTimeoutException, SSLException — exercising one variant covers the catch.
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/login"))
                respond(TestFixtureLoader.load("login"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                throw IOException("Connection refused")
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { errorClient ->
            errorClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }

            assertThat(ex.message)
                .contains("Network error")
                .contains("GET /accounts")
                .contains("Connection refused")
            assertThat(ex.cause).isInstanceOf(IOException::class.java)
        }
    }

    @Test
    fun `wraps HttpRequestTimeoutException as BuxferApiException with method, path, and cause`() = runTest {
        // HttpRequestTimeoutException is fired by Ktor's HttpTimeout plugin in production,
        // but it extends kotlinx.io.IOException (not java.io.IOException) so we can't rely
        // on the IOException catch to handle it. Throw it directly from the engine to verify
        // its dedicated catch arm in `traced`.
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/login"))
                respond(TestFixtureLoader.load("login"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                throw HttpRequestTimeoutException(request.url.toString(), 30_000L)
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { errorClient ->
            errorClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }

            assertThat(ex.message)
                .contains("timed out")
                .contains("GET /accounts")
            assertThat(ex.cause).isInstanceOf(HttpRequestTimeoutException::class.java)
        }
    }

    @Test
    fun `wraps unexpected Exception as BuxferApiException with method, path, and cause`() = runTest {
        // Catch-all for any Ktor engine-level failure that isn't an IOException subclass —
        // e.g. FailToConnectException, configuration errors, plugin failures. RuntimeException
        // stands in for the category.
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/login"))
                respond(TestFixtureLoader.load("login"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            else
                throw RuntimeException("synthetic engine failure")
        }
        BuxferClient(BuxferClientConfig(engine = engine)).use { errorClient ->
            errorClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { errorClient.getAccounts() }

            assertThat(ex.message)
                .contains("Unexpected error")
                .contains("GET /accounts")
                .contains("synthetic engine failure")
            assertThat(ex.cause).isInstanceOf(RuntimeException::class.java)
        }
    }
}
