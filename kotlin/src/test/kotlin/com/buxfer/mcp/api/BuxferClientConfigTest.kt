package com.buxfer.mcp.api

import io.ktor.client.engine.mock.MockEngine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuxferClientConfigTest {

    // Stub engine so the no-args constructor doesn't spin up a real CIO instance per test
    // (which would leak coroutines/threads since BuxferClientConfig doesn't own engine lifecycle).
    private val stubEngine = MockEngine { error("not invoked in config tests") }

    @Test
    fun `baseUrl defaults to DEFAULT_BASE_URL when no override is supplied`() {
        val config = BuxferClientConfig(engine = stubEngine)

        assertThat(config.baseUrl).isEqualTo(BuxferClientConfig.DEFAULT_BASE_URL)
    }

    @Test
    fun `explicit baseUrl argument is respected`() {
        val config = BuxferClientConfig(engine = stubEngine, baseUrl = "https://custom.example.test/api")

        assertThat(config.baseUrl).isEqualTo("https://custom.example.test/api")
    }

    @Test
    fun `timeouts default to documented constants`() {
        val config = BuxferClientConfig(engine = stubEngine)

        assertThat(config.connectTimeoutMillis).isEqualTo(BuxferClientConfig.DEFAULT_CONNECT_TIMEOUT_MS)
        assertThat(config.requestTimeoutMillis).isEqualTo(BuxferClientConfig.DEFAULT_REQUEST_TIMEOUT_MS)
        assertThat(config.socketTimeoutMillis).isEqualTo(BuxferClientConfig.DEFAULT_SOCKET_TIMEOUT_MS)
    }

    @Test
    fun `timeouts are overridable via constructor arguments`() {
        val config = BuxferClientConfig(
            engine = stubEngine,
            connectTimeoutMillis = 1_000,
            requestTimeoutMillis = 2_000,
            socketTimeoutMillis = 3_000,
        )

        assertThat(config.connectTimeoutMillis).isEqualTo(1_000)
        assertThat(config.requestTimeoutMillis).isEqualTo(2_000)
        assertThat(config.socketTimeoutMillis).isEqualTo(3_000)
    }
}
