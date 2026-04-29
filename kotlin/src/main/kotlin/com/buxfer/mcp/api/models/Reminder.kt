package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val startDate: String = "",
    val periodUnit: String = "",
    val amount: Double = 0.0,
    val accountId: Int = 0
)
