package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: Int = 0,
    val name: String = "",
    val email: String = "",
    val balance: Double = 0.0
)
