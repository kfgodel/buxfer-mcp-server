package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

// TODO: Implement Tag data class.
// Fields: id, name, parentId (nullable Int)
// Annotate with @Serializable.

@Serializable
data class Tag(
    val id: Int = 0,
    val name: String = "",
    val parentId: Int? = null
)
