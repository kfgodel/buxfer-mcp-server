package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/reminders` response items. Used by [BuxferClient.validateSchema]
 * to log warnings when Buxfer's response shape changes; not held as data anywhere — the data
 * path forwards the raw `JsonArray` to Claude.
 *
 * Three nullability tiers (per the convention in `kotlin/CLAUDE.md`):
 * - **Required** (key + non-null value, fixture-always-present): `id`, `name`, `description`,
 *   `startDate`, `periodUnit`, `periodSize`, `amount`, `accountId`, `nextExecution`,
 *   `dueDateDescription`, `numDaysForDueDate`, `tags`, `editMode`, `type`, `transactionType`,
 *   `account`.
 * - **Required key, nullable value** (`String?` with no default — key always present in the
 *   fixture, value is `null`): `stopDate`. Absence of the key would be drift.
 * - **Optional, spec-only** (`Type? = null` — documented but never seen in captured
 *   responses): `period` (the spec mentions a singular `period` string; the fixture covers
 *   the same conceptual ground via `periodUnit` + `periodSize`, so `period` may be a retired
 *   spec field).
 *
 * Note on the redundant-looking `type` (string) + `transactionType` (int): both appear in
 * every captured record. `type` carries the human-readable form ("expense") and
 * `transactionType` an integer code (3 in every observed record). Buxfer chose to send both;
 * we trust the fixture and require both. If one ever stops being sent, the validator surfaces
 * it as drift.
 *
 * Note on `account`: Buxfer inlines a full account object (richer than the `/accounts`
 * summary view) on every reminder. Modelled as [EmbeddedAccount] rather than [Account]
 * because the shape differs — `bank` is a structured object here, not a string. See the
 * KDoc on [EmbeddedAccount] for the difference between Buxfer's three account
 * representations.
 */
@Serializable
data class Reminder(
    val id: Int,
    val name: String,
    val description: String,
    val startDate: String,
    val periodUnit: String,
    val periodSize: Int,
    val amount: Double,
    val accountId: Int,
    val nextExecution: String,
    val dueDateDescription: String,
    val numDaysForDueDate: Int,
    val tags: List<Tag>,
    val editMode: String,
    val type: String,
    val transactionType: Int,
    val stopDate: String?,
    val account: EmbeddedAccount,
    val period: String? = null,
)
