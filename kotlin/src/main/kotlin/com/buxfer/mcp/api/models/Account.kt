package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Int = 0,
    val name: String = "",
    val bank: String = "",
    val balance: Double = 0.0,
    val currency: String = ""
)
