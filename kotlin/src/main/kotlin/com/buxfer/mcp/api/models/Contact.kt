package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: Int,
    val name: String? = null,
    val email: String? = null,
    val balance: Double? = null
)
