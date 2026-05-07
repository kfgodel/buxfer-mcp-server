package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/loans` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Loan has no `id` — `entity` is the identity-ish field (group/contact name). All four
 * fields appear on every captured fixture record (3/3 in
 * `shared/test-fixtures/wiremock/__files/loans.json`); the Buxfer API spec is silent on
 * requiredness but fixture evidence is conclusive.
 */
@Serializable
data class Loan(
    val entity: String,
    val type: String,
    val balance: Double,
    val description: String,
)
