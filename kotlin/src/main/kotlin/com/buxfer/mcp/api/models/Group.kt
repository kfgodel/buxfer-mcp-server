package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/groups` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * **Schema is fully unverified.** The captured fixture
 * `shared/test-fixtures/wiremock/__files/groups.json` is an empty array (the test account had
 * no groups), so we have no fixture evidence to tighten any field. Every field lands in
 * Tier 3 (`Type? = null`, optional) per the spec-only-nullable convention; the validator
 * effectively no-ops on Group responses today. **Revisit when a real fixture is captured**
 * and tighten the fields that turn out to be present on every record.
 *
 * Spec-described fields per `shared/api-spec/buxfer-api.md`: `id`, `name`, `consolidated`,
 * `members[]`. The current Kotlin model previously also carried fields not in the spec
 * (kept for now, in case they were observed in real responses we never captured).
 */
@Serializable
data class Group(
    val id: Int? = null,
    val name: String? = null,
    val consolidated: Boolean? = null,
    val members: List<Member>? = null,
)
