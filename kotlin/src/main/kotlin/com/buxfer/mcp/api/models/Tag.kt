package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/tags` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Field nullability reflects what every fixture record contains: `id` / `name` / `relativeName`
 * are present on all 3 captured records (`shared/test-fixtures/wiremock/__files/tags.json`).
 * `parentId` is documented as nullable in the Buxfer API spec and the fixture has both
 * null and Int values, so it stays nullable.
 *
 * Tag is also embedded inside `Budget.tag` and `Reminder.tags`. Tightening here means
 * those still-unmigrated endpoints' strict decode (via `getList`, which uses the production
 * `buxferJson`) will also enforce these required fields. Fixtures back it; the window where
 * a partial nested Tag could throw in production closes as Budget and Reminder migrate to
 * the non-throwing `validateSchema` path (queued in IMPROVEMENT_PLAN.md).
 */
@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val relativeName: String,
    val parentId: Int? = null,
)
