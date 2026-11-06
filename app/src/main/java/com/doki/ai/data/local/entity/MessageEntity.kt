package com.doki.ai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.doki.ai.domain.model.Message
import com.doki.ai.domain.model.MessageRole

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val userId: String?,
    val role: String,
    val content: String,
    val toolCallsJson: String?,
    val toolResultsJson: String?,
    val tokensUsed: Int,
    val createdAt: Long
) {
    fun toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        userId = userId,
        role = MessageRole.fromString(role),
        content = content,
        toolCallsJson = toolCallsJson,
        toolResultsJson = toolResultsJson,
        tokensUsed = tokensUsed,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(msg: Message): MessageEntity = MessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            userId = msg.userId,
            role = msg.role.name.lowercase(),
            content = msg.content,
            toolCallsJson = msg.toolCallsJson,
            toolResultsJson = msg.toolResultsJson,
            tokensUsed = msg.tokensUsed,
            createdAt = msg.createdAt
        )
    }
}

