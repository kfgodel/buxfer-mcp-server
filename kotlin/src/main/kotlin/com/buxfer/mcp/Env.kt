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
 * ## Side effect: Logback substitution bridge
 *
 * Two keys — `BUXFER_LOG_DIR` and `BUXFER_LOG_LEVEL` — are also pushed into
 * JVM system properties via [System.setProperty] (unless already set in
 * either system properties or the OS environment; real env wins). This is
 * the only way to feed values into Logback's `${VAR}` substitution in
 * `logback.xml`, which is a string-template lookup performed at logger
 * initialization. Logback initializes on the first `LoggerFactory.getLogger(...)`
 * call, so callers must invoke [load] before acquiring any SLF4J logger.
 *
 * Every other value (credentials, API base URL) is returned via
 * [BuxferMcpConfig] and passed directly to its destination by the caller.
 * No other system-property side effects.
 */
object Env {

    /**
     * Keys that Logback's `logback.xml` substitution depends on. These are
     * the only entries [load] copies into JVM system properties — every
     * other value flows through [BuxferMcpConfig].
     */
    private val LOGBACK_BRIDGE_KEYS = setOf("BUXFER_LOG_DIR", "BUXFER_LOG_LEVEL")

    /**
     * Parse [args], read the env file, bridge Logback keys to system
     * properties, and return the typed [BuxferMcpConfig].
     *
     * @throws IllegalStateException when the env file is missing, an unknown
     *   CLI argument is supplied, or a required key (`BUXFER_EMAIL`,
     *   `BUXFER_PASSWORD`) is absent from both the file and the OS environment.
     *   The caller (typically [Main][com.buxfer.mcp.Main]) lets it propagate
     *   so the JVM's default uncaught-exception handler prints it on stderr
     *   and exits with code 1 — process management is not Env's concern.
     */
    fun load(args: Array<String> = emptyArray()): BuxferMcpConfig {
        val explicitPath = parseEnvFileArg(args)
        val file = File(explicitPath ?: ".env")
        check(file.isFile) {
            "env file '${file.absolutePath}' does not exist or is not a regular file"
        }

        val props = Properties().apply { file.reader().use { load(it) } }
        bridgeLogbackKeysToSystemProperties(props)

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
     * Bridge the two Logback substitution keys from the file into JVM system
     * properties so `logback.xml`'s `${BUXFER_LOG_DIR}` / `${BUXFER_LOG_LEVEL}`
     * resolves at logger init.
     *
     * This is the only system-property side effect Env produces. Other
     * values flow through [BuxferMcpConfig] and are passed directly by the
     * caller — keeping config-as-arguments the rule, with this narrow
     * exception for the one mechanism (Logback's XML substitution) that
     * isn't programmable.
     *
     * Real env or pre-set system properties always win.
     */
    private fun bridgeLogbackKeysToSystemProperties(props: Properties) {
        for (key in LOGBACK_BRIDGE_KEYS) {
            if (System.getProperty(key) != null) continue
            if (System.getenv(key) != null) continue
            val value = props.getProperty(key) ?: continue
            System.setProperty(key, value)
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
