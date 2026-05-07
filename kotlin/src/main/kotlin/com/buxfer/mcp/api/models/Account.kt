package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/accounts` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Field nullability mixes two sources:
 * - Required fields (`id`, `name`, `bank`, `balance`, `currency`) are present on every
 *   captured fixture record (5/5 in `shared/test-fixtures/wiremock/__files/accounts.json`).
 *   `currency` isn't in the API spec but every record carries one (ARS, USD, EUR observed).
 * - Nullable spec-only fields (`lastSynced`) are documented in the Buxfer API spec but
 *   absent from every captured record. Declaring them keeps the schema forward-compatible
 *   (the validator won't fire if Buxfer starts sending them) and serves as documentation
 *   of what the spec promises.
 */
@Serializable
data class Account(
    val id: Int,
    val name: String,
    val bank: String,
    val balance: Double,
    val currency: String,
    val lastSynced: String? = null,
)
