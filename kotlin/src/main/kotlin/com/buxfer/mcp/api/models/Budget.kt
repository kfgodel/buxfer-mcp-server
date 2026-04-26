package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

// TODO: Implement Budget data class.
// Fields: id, name, limit, remaining, period, currentPeriod, tags (List<String>), keywords (List<String>)
// Annotate with @Serializable.

@Serializable
data class Budget(
    val id: Int = 0,
    val name: String = "",
    val limit: Double = 0.0,
    val remaining: Double = 0.0,
    val period: String = "",
    val currentPeriod: String = "",
    val tags: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)
