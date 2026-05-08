package com.buxfer.mcp

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Tests for [Env] — loads a `.env` file (Java [java.util.Properties] format)
 * into a typed [BuxferMcpConfig], bridging only the two Logback substitution
 * keys (`BUXFER_LOG_DIR`, `BUXFER_LOG_LEVEL`) into JVM system properties so
 * `logback.xml`'s `${VAR}` resolution sees them. Every other value flows
 * through the returned [BuxferMcpConfig].
 *
 * Misconfiguration surfaces as [IllegalStateException]; the JVM's default
 * uncaught-exception handler in production is what turns those into a
 * stderr message + exit 1. Process management is not Env's concern.
 *
 * All tests use `--env-file=<path>` against a temp file so they don't depend
 * on what's in the working directory's `.env` at test time.
 */
class EnvTest {

    /**
     * System property keys we set during a test. Tracked so [tearDown] can
     * clear them — JUnit does not isolate system properties between tests,
     * and a leaked entry would change the meaning of "already set" for
     * whatever runs next.
     */
    private val touchedKeys = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        touchedKeys.clear()
    }

    @AfterEach
    fun tearDown() {
        touchedKeys.forEach(System::clearProperty)
    }

    @Test
    fun `load returns typed config with required and optional fields populated`(@TempDir tmp: File) {
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            BUXFER_API_BASE_URL=https://staging.buxfer.test/api
            BUXFER_LOG_DIR=/var/log/buxfer
            BUXFER_LOG_LEVEL=DEBUG
            """.trimIndent()
        )
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD", "BUXFER_API_BASE_URL", "BUXFER_LOG_DIR", "BUXFER_LOG_LEVEL")

        val config = Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(config.email).isEqualTo("user@example.test")
        assertThat(config.password).isEqualTo("secret")
        assertThat(config.apiBaseUrl).isEqualTo("https://staging.buxfer.test/api")
        assertThat(config.logDir).isEqualTo("/var/log/buxfer")
        assertThat(config.logLevel).isEqualTo("DEBUG")
        assertThat(config.sourcePath).isEqualTo(envFile.absolutePath)
    }

    @Test
    fun `load leaves optional fields null when absent from file`(@TempDir tmp: File) {
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            """.trimIndent()
        )
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD")

        val config = Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(config.apiBaseUrl).isNull()
        assertThat(config.logDir).isNull()
        assertThat(config.logLevel).isNull()
    }

    @Test
    fun `load bridges only Logback substitution keys to system properties`(@TempDir tmp: File) {
        rememberKeys(
            "BUXFER_EMAIL", "BUXFER_PASSWORD",
            "BUXFER_LOG_DIR", "BUXFER_LOG_LEVEL",
            "BUXFER_API_BASE_URL",
        )
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            BUXFER_API_BASE_URL=https://staging.buxfer.test/api
            BUXFER_LOG_DIR=/tmp/buxfer-logs
            BUXFER_LOG_LEVEL=DEBUG
            """.trimIndent()
        )

        Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        // Bridged: logback.xml's ${VAR} substitution needs these as system properties.
        assertThat(System.getProperty("BUXFER_LOG_DIR")).isEqualTo("/tmp/buxfer-logs")
        assertThat(System.getProperty("BUXFER_LOG_LEVEL")).isEqualTo("DEBUG")
        // Not bridged: these flow through BuxferMcpConfig fields only.
        assertThat(System.getProperty("BUXFER_EMAIL")).isNull()
        assertThat(System.getProperty("BUXFER_PASSWORD")).isNull()
        assertThat(System.getProperty("BUXFER_API_BASE_URL")).isNull()
    }

    @Test
    fun `load preserves an already-set Logback system property (real env wins)`(@TempDir tmp: File) {
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD", "BUXFER_LOG_DIR")
        System.setProperty("BUXFER_LOG_DIR", "/preset/by/launcher")
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            BUXFER_LOG_DIR=/from/env/file
            """.trimIndent()
        )

        Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(System.getProperty("BUXFER_LOG_DIR")).isEqualTo("/preset/by/launcher")
    }

    @Test
    fun `load throws when env file does not exist`(@TempDir tmp: File) {
        val missing = tmp.resolve("absent.env")

        assertThatThrownBy { Env.load(arrayOf("--env-file=${missing.absolutePath}")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(missing.absolutePath)
    }

    @Test
    fun `load throws when BUXFER_EMAIL is missing`(@TempDir tmp: File) {
        val envFile = writeEnv(tmp, "BUXFER_PASSWORD=secret")

        assertThatThrownBy { Env.load(arrayOf("--env-file=${envFile.absolutePath}")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("BUXFER_EMAIL")
    }

    @Test
    fun `load throws when BUXFER_PASSWORD is missing`(@TempDir tmp: File) {
        val envFile = writeEnv(tmp, "BUXFER_EMAIL=user@example.test")

        assertThatThrownBy { Env.load(arrayOf("--env-file=${envFile.absolutePath}")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("BUXFER_PASSWORD")
    }

    @Test
    fun `load throws on unknown CLI argument`(@TempDir tmp: File) {
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            """.trimIndent()
        )

        assertThatThrownBy {
            Env.load(arrayOf("--env-file=${envFile.absolutePath}", "--bogus-flag"))
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("--bogus-flag")
    }

    @Test
    fun `load skips blank lines and comments per Properties format`(@TempDir tmp: File) {
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD")
        val envFile = writeEnv(
            tmp,
            """
            # leading comment
            BUXFER_EMAIL=user@example.test

            # mid comment
            BUXFER_PASSWORD=secret
            """.trimIndent()
        )

        val config = Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(config.email).isEqualTo("user@example.test")
        assertThat(config.password).isEqualTo("secret")
    }

    private fun writeEnv(dir: File, content: String): File =
        dir.resolve(".env").apply { writeText(content) }

    private fun rememberKeys(vararg keys: String) {
        touchedKeys.addAll(keys)
    }
}
