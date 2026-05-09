package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/tags` response items, also reused for the nested `tag`
 * field on `Budget` and the `tags[]` field on `Reminder`. Used by
 * [BuxferClient.validateSchema] to log warnings when Buxfer's response shape changes;
 * not held as data anywhere — the data path forwards the raw `JsonArray` to Claude.
 *
 * Three nullability tiers (per the convention in `kotlin/CLAUDE.md`):
 * - **Required** (key + non-null value, fixture-always-present): `id`, `name`,
 *   `relativeName`.
 * - **Required key, nullable value** (`Int?` with no default): `parentId`. The key is
 *   always present in the captured fixture; the value is `null` for root tags and an
 *   `Int` for child tags. Absence of the key entirely would be drift.
 */
@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val relativeName: String,
    val parentId: Int?,
)
