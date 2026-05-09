package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/contacts` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Three nullability tiers (per the convention in `kotlin/CLAUDE.md`):
 * - **Required** (key + non-null value): `id`, `name`, `balance` — present on every captured
 *   fixture record with non-null values.
 * - **Required key, nullable value** (`String?` with no default): `email`. The captured
 *   fixture has both populated and `null` emails (Contacts 3 and 4 carry `null`); the live
 *   Buxfer API emits `null` for contacts that have no email on file. The key is always
 *   present, only the value varies.
 */
@Serializable
data class Contact(
    val id: Int,
    val name: String,
    val email: String?,
    val balance: Double,
)
