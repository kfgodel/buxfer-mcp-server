package com.buxfer.mcp

import java.io.File
import java.util.Properties

/**
 * Loads runtime configuration for the Buxfer MCP server from an env file
 * into a typed [BuxferMcpConfig].
 *
 * ## File location
 *
 * Either:
 *   1. The path given via the `--env-file=<path>` CLI flag, or
 *   2. `./.env` (relative to the JVM's working directory) if no flag is supplied.
 *
 * No directory walking, no fallback chains. If the file isn't where one of
 * those two places looks, the launcher needs to pass `--env-file=`.
 *
 * ## File format
 *
 * Java [Properties] format: `KEY=VALUE`, `#` comments, blank lines, whitespace
 * trimmed. Backslashes are escape characters per the Properties spec — values
 * with literal backslashes must double them (`\\`). Quoting is **not** stripped.
 *
 * ## Side effect: system properties
 *
 * Every entry from the file is also pushed into JVM system properties via
 * [System.setProperty], unless the key is already set in either system
 * properties or the OS environment (real env wins). This is what lets
 * `logback.xml`'s `${BUXFER_LOG_DIR}` / `${BUXFER_LOG_LEVEL}` substitution
 * resolve correctly — Logback reads system properties at first-logger-call
 * time, which is why callers must invoke [load] before acquiring any SLF4J
 * logger.
 *
 * The system-property push also keeps [com.buxfer.mcp.api.BuxferClientConfig]
 * happy: its `BUXFER_API_BASE_URL` lookup hits `System.getProperty` first.
 */
object Env {

    /**
     * Parse [args], read the env file, populate system properties, and return
     * the typed [BuxferMcpConfig].
     *
     * @throws IllegalStateException when the env file is missing, an unknown
     *   CLI argument is supplied, or a required key (`BUXFER_EMAIL`,
     *   `BUXFER_PASSWORD`) is absent from both the file and the OS environment.
     */
    fun load(args: Array<String> = emptyArray()): BuxferMcpConfig {
        val explicitPath = parseEnvFileArg(args)
        val file = File(explicitPath ?: ".env")
        check(file.isFile) {
            "env file '${file.absolutePath}' does not exist or is not a regular file"
        }

        val props = Properties().apply { file.reader().use { load(it) } }
        applyToSystemProperties(props)

        return BuxferMcpConfig(
            email = readRequired(props, "BUXFER_EMAIL"),
            password = readRequired(props, "BUXFER_PASSWORD"),
            apiBaseUrl = readOptional(props, "BUXFER_API_BASE_URL"),
            logDir = readOptional(props, "BUXFER_LOG_DIR"),
            logLevel = readOptional(props, "BUXFER_LOG_LEVEL"),
            sourcePath = file.absolutePath,
        )
    }

    /**
     * Pull `--env-file=<path>` out of [args]. Throws on any other argument so
     * typos surface loudly instead of being silently ignored.
     */
    private fun parseEnvFileArg(args: Array<String>): String? {
        var path: String? = null
        for (arg in args) {
            check(arg.startsWith("--env-file=")) {
                "Unknown argument: '$arg'. Usage: java -jar buxfer-mcp-server-all.jar [--env-file=<path>]"
            }
            path = arg.substringAfter("=").takeIf { it.isNotBlank() }
        }
        return path
    }

    /**
     * Promote each file entry into JVM system properties unless the key is
     * already set in the OS environment or system properties. Real env always
     * wins over the file — standard precedence.
     */
    private fun applyToSystemProperties(props: Properties) {
        for (key in props.stringPropertyNames()) {
            if (System.getProperty(key) != null) continue
            if (System.getenv(key) != null) continue
            System.setProperty(key, props.getProperty(key))
        }
    }

    private fun readRequired(props: Properties, key: String): String {
        val value = props.getProperty(key) ?: System.getenv(key)
        check(!value.isNullOrBlank()) { "$key is required but missing" }
        return value
    }

    private fun readOptional(props: Properties, key: String): String? =
        (props.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }
}
