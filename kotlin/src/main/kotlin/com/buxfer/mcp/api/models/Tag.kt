package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Int,
    val name: String? = null,
    val relativeName: String? = null,
    val parentId: Int? = null
)
