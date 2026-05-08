package com.buxfer.mcp

/**
 * Typed view of the runtime configuration for the Buxfer MCP server, loaded
 * from an env file by [Env.load].
 *
 * Distinct from [com.buxfer.mcp.api.BuxferClientConfig], which configures the
 * HTTP client (timeouts, base URL, engine). This one is the operator-facing
 * config that comes out of `.env` — credentials and logging knobs.
 *
 * Required fields are non-null; optional fields are nullable so callers can
 * fall through to defaults defined elsewhere (Logback's `${VAR:-default}`
 * substitution, [BuxferClientConfig.DEFAULT_BASE_URL], etc.).
 *
 * @property email Buxfer account email. Required.
 * @property password Buxfer account password. Required.
 * @property apiBaseUrl Override for the Buxfer REST API base URL. `null` means
 *   "use the default in `BuxferClientConfig`".
 * @property logDir Override for the rolling log appender's output directory.
 *   `null` means "use Logback's `${BUXFER_LOG_DIR:-./logs}` default".
 * @property logLevel Override for the Logback root level. `null` means "use
 *   Logback's `${BUXFER_LOG_LEVEL:-INFO}` default".
 * @property sourcePath Absolute path of the env file the values came from.
 *   Used purely for operator-visible logging (e.g. "loaded config from …").
 */
data class BuxferMcpConfig(
    val email: String,
    val password: String,
    val apiBaseUrl: String?,
    val logDir: String?,
    val logLevel: String?,
    val sourcePath: String,
)
