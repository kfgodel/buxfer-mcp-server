package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Int,
    val name: String? = null,
    val bank: String? = null,
    val balance: Double? = null,
    val currency: String? = null
)
