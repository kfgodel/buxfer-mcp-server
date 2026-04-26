package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

// TODO: Implement Reminder data class.
// Fields: id, name, startDate, period, amount, accountId
// Annotate with @Serializable.

@Serializable
data class Reminder(
    val id: Int = 0,
    val name: String = "",
    val startDate: String = "",
    val period: String = "",
    val amount: Double = 0.0,
    val accountId: Int = 0
)
