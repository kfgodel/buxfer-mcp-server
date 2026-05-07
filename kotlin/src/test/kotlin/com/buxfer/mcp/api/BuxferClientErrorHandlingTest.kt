package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
import com.buxfer.mcp.testing.MockEngineSupport
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
        // Account.id is the identity field — required, no default. If the API drops it,
        // kotlinx.serialization throws SerializationException naming the missing field
        // and type, and BuxferClient.traced wraps it as BuxferApiException with method+path
        // context. Together that's enough to diagnose real API drift on sight.
        val engine = MockEngineSupport.newEngine(overrides = mapOf(
            "/accounts" to """{"response":{"status":"OK","accounts":[{"name":"x"}]}}"""
        ))
        BuxferClient(BuxferClientConfig(engine = engine)).use { parseClient ->
            parseClient.login("user@example.com", "password")

            val ex = assertThrows<BuxferApiException> { parseClient.getAccounts() }

            assertThat(ex.message)
                .contains("GET /accounts")
                .contains("id")
                .contains("Account")
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
}
