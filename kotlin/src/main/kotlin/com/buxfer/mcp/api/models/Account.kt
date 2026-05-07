package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/accounts` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Field nullability reflects what we actually observe: every fixture record has all five
 * fields (5/5 in `shared/test-fixtures/wiremock/__files/accounts.json`). The Buxfer API doc
 * doesn't mention `currency` but every captured record carries one (ARS, USD, EUR observed),
 * so it stays in the schema. The doc mentions `lastSynced` but no captured record carries it,
 * so it's deliberately omitted. Adjust if either assumption changes empirically.
 */
@Serializable
data class Account(
    val id: Int,
    val name: String,
    val bank: String,
    val balance: Double,
    val currency: String,
)
