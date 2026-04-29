package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: Int = 0,
    val name: String = "",
    val limit: Double = 0.0,
    val spent: Double = 0.0,
    val balance: Double = 0.0,
    val period: String = "",
    val periodUnit: String = ""
)
