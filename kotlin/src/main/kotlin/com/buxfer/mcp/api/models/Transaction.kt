package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Read-only Buxfer transaction shape. The matching write shape is [AddTransactionParams].
 *
 * Note: the API emits both `tags` (comma-separated string) and `tagNames` (array of names)
 * for each transaction; both are documented response fields. The string form mirrors the
 * input format used by [AddTransactionParams.tags]; the array is convenient for callers
 * that want to enumerate without splitting.
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
    val accountId: Int? = null,
    val accountName: String? = null,
    val transactionType: String? = null,
    val expenseAmount: Double? = null,
    val tagNames: List<String>? = null,
    val isFutureDated: Boolean? = null,
    val isPending: Boolean? = null,
    val fromAccount: AccountRef? = null,
    val toAccount: AccountRef? = null
)
