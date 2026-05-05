package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val id: Int,
    val name: String,
    val email: String,
    val balance: Double
)
