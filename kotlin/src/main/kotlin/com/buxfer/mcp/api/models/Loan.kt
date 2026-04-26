package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

// TODO: Implement Loan data class.
// Fields: entity, type ("group" or "contact"), balance, description
// Annotate with @Serializable.

@Serializable
data class Loan(
    val entity: String = "",
    val type: String = "",
    val balance: Double = 0.0,
    val description: String = ""
)
