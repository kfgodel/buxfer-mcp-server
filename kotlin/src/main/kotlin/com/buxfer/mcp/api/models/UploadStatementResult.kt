package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for the `/upload_statement` write-endpoint response. Used by
 * [BuxferClient.validateSchema]; not held as data — the data path forwards the raw
 * `JsonObject` to Claude.
 *
 * Both fields are required per the Buxfer API spec. The captured fixture
 * `shared/test-fixtures/wiremock/__files/upload_statement.json` is empty (`{}`) — see
 * `api-recordings/CLAUDE.md` for the manual capture workflow. The test stack
 * works around the empty fixture via `MockEngineSupport.UPLOAD_STATEMENT_OK_BODY`,
 * which carries both fields with realistic values. Tighten / relax fields here once
 * a real fixture is captured.
 */
@Serializable
data class UploadStatementResult(
    val uploaded: Int,
    val balance: Double,
)
