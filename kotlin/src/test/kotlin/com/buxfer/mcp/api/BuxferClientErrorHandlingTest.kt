package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.api.models.AddTransactionParams
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
        // Transaction.id is the identity field — required, no default. The write endpoints
        // (addTransaction / editTransaction / uploadStatement) still decode their responses
        // into typed Kotlin models, so a payload missing required fields throws
        // SerializationException, which `BuxferClient.traced` wraps as BuxferApiException
        // with method+path context.
        //
        // Note: all GET list endpoints (`/accounts`, `/tags`, `/budgets`, …) have migrated
        // to the non-throwing `validateSchema` path — drift on those is covered by
        // `getAccounts logs schema-drift warning on missing required field`. This test
        // exercises the strict-decode-throw path that remains live for write endpoints.
        val engine = MockEngineSupport.newEngine(overrides = mapOf(
            "/transaction_add" to """{"response":{"name":"x"}}"""
        ))
        BuxferClient(BuxferClientConfig(engine = engine)).use { parseClient ->
            parseClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> {
                parseClient.addTransaction(
                    AddTransactionParams("Test", 0.01, 10350, "2026-04-26")
                )
            }

            assertThat(ex.message)
                .contains("POST /transaction_add")
                .contains("id")
                .contains("Transaction")
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
