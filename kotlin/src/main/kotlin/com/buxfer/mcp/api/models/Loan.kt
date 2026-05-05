package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Loan(
    val entity: String = "",
    val type: String = "",
    val balance: Double = 0.0,
    val description: String = ""
)
