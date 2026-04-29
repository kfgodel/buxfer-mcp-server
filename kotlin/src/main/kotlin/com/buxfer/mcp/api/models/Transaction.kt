package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AccountRef(
    val id: Int = 0,
    val name: String = ""
)

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

data class TransactionFilters(
    val accountId: Int? = null,
    val accountName: String? = null,
    val tagId: Int? = null,
    val tagName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val budgetId: Int? = null,
    val budgetName: String? = null,
    val contactId: Int? = null,
    val contactName: String? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
    val status: String? = null,
    val page: Int? = null
)

data class AddTransactionParams(
    val description: String,
    val amount: Double,
    val accountId: Int,
    val date: String,
    val tags: String? = null,
    val type: String? = null,
    val status: String? = null
)

data class TransactionsResult(
    val transactions: List<Transaction>,
    val numTransactions: Int
)

@Serializable
data class UploadStatementResult(
    val uploaded: Int,
    val balance: Double
)
