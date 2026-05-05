package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Int,
    val name: String,
    val limit: Double,
    val spent: Double,
    val balance: Double,
    val period: String,
    val periodUnit: String,
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
