package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Reference to an account, embedded in [Transaction.fromAccount] and [Transaction.toAccount]
 * on transfer-type transactions.
 *
 * Both fields are required: every transfer record in
 * `shared/test-fixtures/wiremock/__files/transactions.json` (1/1) has both `id` and `name`
 * with non-null values.
 */
@Serializable
data class AccountRef(
    val id: Int,
    val name: String,
)
