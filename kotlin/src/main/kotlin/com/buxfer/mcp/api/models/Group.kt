package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int = 0,
    val name: String = "",
    val consolidated: Boolean = false,
    val members: List<Member> = emptyList()
)
