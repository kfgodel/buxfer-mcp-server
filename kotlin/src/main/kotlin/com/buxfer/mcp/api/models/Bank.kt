package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for the bank object nested inside [EmbeddedAccount]
 * (which itself is nested inside `/reminders` response items, and likely other
 * places where Buxfer inlines a full account view). Used by
 * [com.buxfer.mcp.api.BuxferClient.validateSchema]; not held as data anywhere
 * — the data path forwards the raw JSON to Claude.
 *
 * Distinct from the simpler `bank` field on the top-level [Account] model,
 * which Buxfer emits as a plain string in `/accounts` responses. Here the
 * same conceptual entity is a structured object with its own metadata.
 *
 * Three nullability tiers (per the convention in `kotlin/CLAUDE.md`):
 * - **Required** (key + non-null value, fixture-always-present): `id`,
 *   `name`, `canSync`, `requireVerifiedEmail`, `requireRedirect`, `status`,
 *   `needsSyncMigration`.
 * - **Required key, nullable value** (`Type?` with no default — key always
 *   present in the fixture, value is `null`): `syncProvider`,
 *   `migrationTargetBankId`. Buxfer emits these slots even when the bank
 *   has no linked sync provider or migration target.
 */
@Serializable
data class Bank(
    val id: Int,
    val name: String,
    val canSync: Boolean,
    val requireVerifiedEmail: Boolean,
    val requireRedirect: Boolean,
    val syncProvider: String?,
    val status: Int,
    val needsSyncMigration: Boolean,
    val migrationTargetBankId: Int?,
)
