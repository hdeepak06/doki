package com.doki.ai.domain.model

import java.util.UUID

data class CustomCommand(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val triggerPhrase: String,
    val actionType: String = "tool_chain", // 'routine', 'intent', 'tool_chain'
    val actionPayload: String, // JSON description of actions
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

