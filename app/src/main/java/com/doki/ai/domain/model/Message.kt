package com.doki.ai.domain.model

import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    TOOL;

    companion object {
        fun fromString(role: String): MessageRole = when (role.lowercase()) {
            "user" -> USER
            "assistant", "model" -> ASSISTANT
            "system" -> SYSTEM
            "tool" -> TOOL
            else -> ASSISTANT
        }
    }
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val userId: String? = null,
    val role: MessageRole,
    val content: String,
    val toolCallsJson: String? = null,
    val toolResultsJson: String? = null,
    val isStreaming: Boolean = false,
    val tokensUsed: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

