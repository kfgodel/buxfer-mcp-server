package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for the `/transactions` response wrapper. Used by
 * [BuxferClient.validateSchema]; not held as data — the data path forwards the raw
 * `JsonObject` (with both `transactions` and `numTransactions` fields) to Claude.
 *
 * `numTransactions` is declared as **String** to match the wire format — Buxfer returns
 * it quoted (e.g. `"numTransactions": "5"`) rather than as a JSON number. The schema mirrors
 * the wire so the validator doesn't fire on this established quirk; the LLM sees the same
 * string and can interpret it numerically as needed.
 */
@Serializable
data class TransactionsResult(
    val transactions: List<Transaction>,
    val numTransactions: String,
)
