package com.doki.ai.domain.model

import java.util.UUID

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String = "New Conversation",
    val interactionType: String = "voice", // 'voice' or 'chat'
    val languageUsed: String = "en-US",
    val isPinned: Boolean = false,
    val messageCount: Int = 0,
    val lastMessagePreview: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

