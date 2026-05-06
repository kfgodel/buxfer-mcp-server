package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class TransactionsResult(
    val transactions: List<Transaction>,
    val numTransactions: Int
)
