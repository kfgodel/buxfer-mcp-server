package com.buxfer.mcp.api.models

/**
 * Request parameters for `POST /api/transaction_add` and `POST /api/transaction_edit`.
 *
 * `type` is non-null and required: the upstream docs at
 * https://www.buxfer.com/help/api#transaction_add list it as optional with a
 * default of `expense`, but the live API rejects requests without it
 * (`HTTP 400: Missing value for parameter [type]`). See the live-API
 * divergence callout in `shared/api-spec/buxfer-api.md`.
 */
data class AddTransactionParams(
    val description: String,
    val amount: Double,
    val accountId: Int,
    val date: String,
    val type: String,
    val tags: String? = null,
    val status: String? = null,
)
