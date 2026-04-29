package com.buxfer.mcp.api.models

data class AddTransactionParams(
    val description: String,
    val amount: Double,
    val accountId: Int,
    val date: String,
    val tags: String? = null,
    val type: String? = null,
    val status: String? = null
)
