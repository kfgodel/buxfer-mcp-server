package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

// TODO: Implement Group and Member data classes.
// Group fields: id, name, consolidated, members (List<Member>)
// Member fields: name, balance
// Annotate both with @Serializable.

@Serializable
data class Member(
    val name: String = "",
    val balance: Double = 0.0
)

@Serializable
data class Group(
    val id: Int = 0,
    val name: String = "",
    val consolidated: Boolean = false,
    val members: List<Member> = emptyList()
)
