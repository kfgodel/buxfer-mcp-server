package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Int,
    val name: String,
    val bank: String,
    val balance: Double,
    val currency: String
)
