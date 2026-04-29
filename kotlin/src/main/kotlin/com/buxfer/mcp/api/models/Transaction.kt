package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: Int = 0,
    val description: String = "",
    val amount: Double = 0.0,
    val accountId: Int? = null,
    val accountName: String = "",
    val date: String = "",
    val tags: String = "",
    val type: String = "expense",
    val status: String = "cleared",
    val transactionType: String? = null,
    val expenseAmount: Double? = null,
    val tagNames: List<String>? = null,
    val isFutureDated: Boolean? = null,
    val isPending: Boolean? = null,
    val fromAccount: AccountRef? = null,
    val toAccount: AccountRef? = null
)
