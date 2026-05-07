package com.buxfer.mcp.api

import com.buxfer.mcp.TestFixtureLoader
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

    private fun mockEngine(overrides: Map<String, String> = emptyMap()) = MockEngine { request ->
        val path = request.url.encodedPath
        val body = overrides.entries.firstOrNull { path.endsWith(it.key) }?.value
            ?: when {
                path.endsWith("/login") -> TestFixtureLoader.load("login")
                path.endsWith("/accounts") -> TestFixtureLoader.load("accounts")
                path.endsWith("/transactions") -> TestFixtureLoader.load("transactions")
                path.endsWith("/transaction_add") -> TestFixtureLoader.load("transaction_add")
                path.endsWith("/transaction_edit") -> TestFixtureLoader.load("transaction_edit")
                path.endsWith("/transaction_delete") -> TestFixtureLoader.load("transaction_delete")
                path.endsWith("/upload_statement") -> """{"response":{"status":"OK","uploaded":15,"balance":1234.56}}"""
                path.endsWith("/tags") -> TestFixtureLoader.load("tags")
                path.endsWith("/budgets") -> TestFixtureLoader.load("budgets")
                path.endsWith("/reminders") -> TestFixtureLoader.load("reminders")
                path.endsWith("/groups") -> TestFixtureLoader.load("groups")
                path.endsWith("/contacts") -> TestFixtureLoader.load("contacts")
                path.endsWith("/loans") -> TestFixtureLoader.load("loans")
                else -> """{"response":{"status":"error","error":"Not found"}}"""
            }
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    @Test
    fun `throws BuxferApiException on non-OK status`() = runTest {
        BuxferClient(BuxferClientConfig(engine = mockEngine(overrides = mapOf(
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
        val engine = mockEngine(overrides = mapOf(
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
