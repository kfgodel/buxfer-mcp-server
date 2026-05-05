package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val startDate: String? = null,
    val periodUnit: String? = null,
    val amount: Double? = null,
    val accountId: Int? = null,
    val nextExecution: String? = null,
    val dueDateDescription: String? = null,
    val numDaysForDueDate: Int? = null,
    val tags: List<Tag> = emptyList(),
    val editMode: String? = null,
    val periodSize: Int? = null,
    val stopDate: String? = null,
    val type: String? = null,
    val transactionType: Int? = null
)
