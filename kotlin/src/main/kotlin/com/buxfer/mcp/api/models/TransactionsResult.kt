package com.buxfer.mcp.api.models

data class TransactionsResult(
    val transactions: List<Transaction>,
    val numTransactions: Int
)
