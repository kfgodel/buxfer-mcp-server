package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for members nested inside a [Group]. **Fully unverified** — the
 * captured `groups.json` fixture is empty, so no member structure has ever been observed.
 *
 * Per the Buxfer API spec, a Member is `{name, balance}` (both required); the current Kotlin
 * model also carries `id` and `email` from earlier guesses. All fields land in Tier 3
 * (`Type? = null`, optional) until a real fixture lets us tighten with evidence.
 */
@Serializable
data class Member(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val balance: Double? = null,
)
