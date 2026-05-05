package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val relativeName: String,
    val parentId: Int? = null
)
