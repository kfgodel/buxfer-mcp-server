package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val name: String = "",
    val balance: Double = 0.0
)
