package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/budgets` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * The Buxfer API spec diverges noticeably from what's actually returned. Required fields
 * follow the fixture (2/2 records); spec-described fields the fixture never carries are
 * declared nullable so the schema stays forward-compatible without firing warnings on their
 * absence.
 *
 * Three nullability tiers (per the convention in IMPROVEMENT_PLAN.md):
 * - **Required** (key + non-null value, fixture-always-present): `id`, `name`, `limit`,
 *   `spent`, `balance`, `period`, `periodUnit`, `editMode`, `periodSize`, `startDate`,
 *   `budgetId`, `type`, `tagId`, `tag`, `isRolledOver`, `eventId`.
 * - **Required key, nullable value** (`String?` with no default — key always present in
 *   the fixture, value is `null`): `stopDate`. Absence of the key would be drift.
 * - **Optional, spec-only** (`Type? = null` — documented but never seen in captured
 *   responses; key may be absent entirely): `remaining` (≈ `balance` + `spent`),
 *   `currentPeriod` (≈ `period` + `periodSize` + `periodUnit`), `tags` (spec array vs.
 *   fixture's single `tag` object), `keywords` (no fixture replacement — possibly retired).
 *
 * Notes:
 * - `budgetId` duplicates `id` in every captured record. Both kept required by fixture rule.
 * - `tag` is now non-nullable, leaning on the already-tightened `Tag` schema.
 */
@Serializable
data class Budget(
    val id: Int,
    val name: String,
    val limit: Double,
    val spent: Double,
    val balance: Double,
    val period: String,
    val periodUnit: String,
    val editMode: String,
    val periodSize: Int,
    val startDate: String,
    val stopDate: String?,
    val budgetId: Int,
    val type: Int,
    val tagId: Int,
    val tag: Tag,
    val isRolledOver: Int,
    val eventId: Int,
    val remaining: Double? = null,
    val currentPeriod: String? = null,
    val tags: List<Tag>? = null,
    val keywords: List<String>? = null,
)
