package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Int,
    val name: String? = null,
    val limit: Double? = null,
    val spent: Double? = null,
    val balance: Double? = null,
    val period: String? = null,
    val periodUnit: String? = null,
    val editMode: String? = null,
    val periodSize: Int? = null,
    val startDate: String? = null,
    val stopDate: String? = null,
    val budgetId: Int? = null,
    val type: Int? = null,
    val tagId: Int? = null,
    val tag: Tag? = null,
    val isRolledOver: Int? = null,
    val eventId: Int? = null
)
