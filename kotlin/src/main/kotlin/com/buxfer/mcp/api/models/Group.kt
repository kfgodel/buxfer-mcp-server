package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val consolidated: Boolean,
    val members: List<Member> = emptyList()
)
