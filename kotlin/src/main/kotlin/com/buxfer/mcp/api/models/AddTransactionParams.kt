package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Request parameters for `POST /api/transaction_add` and `POST /api/transaction_edit`.
 *
 * `type` is non-null and required: the upstream docs at
 * https://www.buxfer.com/help/api#transaction_add list it as optional with a
 * default of `expense`, but the live API rejects requests without it
 * (`HTTP 400: Missing value for parameter [type]`). See the live-API
 * divergence callout in `shared/api-spec/buxfer-api.md`.
 *
 * Type-specific extra fields per `shared/api-spec/buxfer-api.md` lines 147-167:
 *  - `sharedBill`:    `payers`, `sharers`, `isEvenSplit`
 *  - `loan`:          `loanedBy`, `borrowedBy`
 *  - `paidForFriend`: `paidBy`, `paidFor`
 *
 * All extra fields are nullable and only sent to Buxfer when populated — the
 * server validates field/type coherence (e.g. `sharers` is rejected for plain
 * `expense`), so the MCP layer forwards what the caller provides without
 * cross-field validation.
 */
data class AddTransactionParams(
    val description: String,
    val amount: Double,
    val accountId: Int,
    val date: String,
    val type: String,
    val tags: String? = null,
    val status: String? = null,
    // sharedBill extras
    val payers: List<PayerShare>? = null,
    val sharers: List<PayerShare>? = null,
    val isEvenSplit: Boolean? = null,
    // loan extras
    val loanedBy: String? = null,
    val borrowedBy: String? = null,
    // paidForFriend extras
    val paidBy: String? = null,
    val paidFor: String? = null,
)

/**
 * A single participant share in a `sharedBill` transaction. Buxfer's API accepts
 * either `email` or `name` to identify the participant; this MCP layer only
 * exposes `email` to keep the tool surface small and unambiguous.
 *
 * `amount` is optional: when `isEvenSplit = true`, Buxfer divides the total
 * evenly across `sharers` and the per-share amount is omitted on the wire.
 *
 * `@Serializable` is required so [com.buxfer.mcp.api.buxferJson].encodeToString
 * can produce the JSON-array string Buxfer expects in the form-encoded
 * `payers` / `sharers` fields.
 */
@Serializable
data class PayerShare(
    val email: String,
    val amount: Double? = null,
)
