package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for `/transactions` response items (and write-endpoint responses
 * for `/transaction_add` / `/transaction_edit`, which decode into the same shape via
 * [BuxferClient.addTransaction] / [BuxferClient.editTransaction]). Used by
 * [BuxferClient.validateSchema] for `/transactions` and as the typed return for the write
 * endpoints; not held as data anywhere in the read path — the data path forwards the raw
 * `JsonObject` to Claude.
 *
 * Three nullability tiers (per the convention in IMPROVEMENT_PLAN.md):
 * - **Required** (key + non-null value, fixture-always-present on every record including
 *   transfers): `id`, `description`, `amount`, `date`, `tags`, `type`, `status`,
 *   `accountName`, `transactionType`, `expenseAmount`, `tagNames`, `isFutureDated`,
 *   `isPending`.
 * - **Required key, nullable value** (`Int?` with no default — key always present in the
 *   fixture, but the transfer record sets it to `null`): `accountId`. Transfers don't
 *   belong to a single account; the API still emits the key.
 * - **Optional, conditional on type=transfer** (`AccountRef? = null`): `fromAccount`,
 *   `toAccount`. Present only on transfer-type records; absent from non-transfer records.
 *
 * Note: the API emits both `tags` (comma-separated string) and `tagNames` (array of names);
 * both are documented response fields. The string form mirrors the input format used by
 * [AddTransactionParams.tags]; the array is convenient for callers that want to enumerate
 * without splitting.
 */
@Serializable
data class Transaction(
    val id: Int,
    val description: String,
    val amount: Double,
    val date: String,
    val tags: String,
    val type: String,
    val status: String,
    val accountName: String,
    val transactionType: String,
    val expenseAmount: Double,
    val tagNames: List<String>,
    val isFutureDated: Boolean,
    val isPending: Boolean,
    val accountId: Int?,
    val fromAccount: AccountRef? = null,
    val toAccount: AccountRef? = null,
)
