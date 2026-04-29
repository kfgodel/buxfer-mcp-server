package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class AccountRef(
    val id: Int = 0,
    val name: String = ""
)
