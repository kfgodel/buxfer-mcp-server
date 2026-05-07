package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/contacts` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * All four fields appear on every captured fixture record (4/4 in
 * `shared/test-fixtures/wiremock/__files/contacts.json`); the Buxfer API spec is silent on
 * requiredness but fixture evidence is conclusive.
 */
@Serializable
data class Contact(
    val id: Int,
    val name: String,
    val email: String,
    val balance: Double,
)
