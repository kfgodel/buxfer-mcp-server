package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

@Serializable
data class UploadStatementResult(
    val uploaded: Int,
    val balance: Double
)
