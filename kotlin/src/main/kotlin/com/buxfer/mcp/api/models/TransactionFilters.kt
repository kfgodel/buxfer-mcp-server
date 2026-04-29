package com.buxfer.mcp.api.models

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
