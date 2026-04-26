package com.buxfer.mcp.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: Implement Transaction, TransactionFilters, AddTransactionParams,
//       and UploadStatementResponse data classes.
//
// Transaction fields: id, description, amount, accountId, date, tags, type, status
// TransactionFilters: accountId?, accountName?, tagId?, tagName?, startDate?, endDate?,
//                     budgetId?, budgetName?, contactId?, contactName?,
//                     groupId?, groupName?, status?, page?
// AddTransactionParams: description, amount, accountId, date, tags?, type?, status?,
//                       plus optional sharedBill/loan/paidForFriend fields
//
// Annotate with @Serializable. Use @SerialName where JSON keys differ from Kotlin naming.
// See shared/api-spec/buxfer-api.md for full field details.

@Serializable
data class Transaction(
    val id: Int = 0,
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("accountId") val accountId: Int = 0,
    val date: String = "",
    val tags: String = "",
    val type: String = "expense",
    val status: String = "cleared"
)

// TODO: Add TransactionFilters, AddTransactionParams, TransactionsResponse,
//       UploadStatementResponse
