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
 * into a typed [BuxferMcpConfig] and pushes its entries into JVM system
 * properties so Logback's `${BUXFER_LOG_DIR}` substitution and downstream
 * `System.getProperty` lookups in [com.buxfer.mcp.api.BuxferClientConfig]
 * resolve correctly.
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
    fun `load pushes file entries into system properties`(@TempDir tmp: File) {
        val key = uniqueKey("PUSH")
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD", key)
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            $key=via-env-file
            """.trimIndent()
        )

        Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(System.getProperty(key)).isEqualTo("via-env-file")
    }

    @Test
    fun `load preserves existing system properties (real env wins)`(@TempDir tmp: File) {
        val key = uniqueKey("PRESERVE")
        rememberKeys("BUXFER_EMAIL", "BUXFER_PASSWORD", key)
        System.setProperty(key, "set-before-load")
        val envFile = writeEnv(
            tmp,
            """
            BUXFER_EMAIL=user@example.test
            BUXFER_PASSWORD=secret
            $key=should-not-overwrite
            """.trimIndent()
        )

        Env.load(arrayOf("--env-file=${envFile.absolutePath}"))

        assertThat(System.getProperty(key)).isEqualTo("set-before-load")
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

    private fun uniqueKey(suffix: String): String =
        "BUXFER_TEST_${System.nanoTime()}_$suffix"

    private fun rememberKeys(vararg keys: String) {
        touchedKeys.addAll(keys)
    }
}
