package com.doki.ai.core.ai.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ToolParameter(
    val name: String,
    val type: String, // "string", "integer", "boolean", "number"
    val description: String,
    val required: Boolean = true
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val requiresConfirmation: Boolean = false
)

