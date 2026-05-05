package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Loan(
    val entity: String,
    val type: String? = null,
    val balance: Double? = null,
    val description: String? = null
)
