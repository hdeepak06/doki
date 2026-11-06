package com.doki.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.doki.ai.domain.model.Conversation

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val title: String,
    val interactionType: String,
    val languageUsed: String,
    val isPinned: Boolean,
    val messageCount: Int,
    val lastMessagePreview: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        userId = userId,
        title = title,
        interactionType = interactionType,
        languageUsed = languageUsed,
        isPinned = isPinned,
        messageCount = messageCount,
        lastMessagePreview = lastMessagePreview,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(conv: Conversation): ConversationEntity = ConversationEntity(
            id = conv.id,
            userId = conv.userId,
            title = conv.title,
            interactionType = conv.interactionType,
            languageUsed = conv.languageUsed,
            isPinned = conv.isPinned,
            messageCount = conv.messageCount,
            lastMessagePreview = conv.lastMessagePreview,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt
        )
    }
}

