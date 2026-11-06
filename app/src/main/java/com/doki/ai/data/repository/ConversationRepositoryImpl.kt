package com.doki.ai.data.repository

import com.doki.ai.data.local.dao.ConversationDao
import com.doki.ai.data.local.dao.MessageDao
import com.doki.ai.data.local.entity.ConversationEntity
import com.doki.ai.data.local.entity.MessageEntity
import com.doki.ai.data.remote.SupabaseClient
import com.doki.ai.domain.model.Conversation
import com.doki.ai.domain.model.Message
import com.doki.ai.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val supabaseClient: SupabaseClient
) : ConversationRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return conversationDao.getConversationById(id)?.toDomain()
    }

    override suspend fun createConversation(conversation: Conversation) {
        val entity = ConversationEntity.fromDomain(conversation)
        conversationDao.insertConversation(entity)

        if (supabaseClient.isConfigured) {
            runCatching {
                val json = buildJsonObject {
                    put("id", conversation.id)
                    put("title", conversation.title)
                    put("interaction_type", conversation.interactionType)
                    put("language_used", conversation.languageUsed)
                    put("is_pinned", conversation.isPinned)
                }.toString()
                supabaseClient.post("conversations", json)
            }
        }
    }

    override suspend fun updateConversation(conversation: Conversation) {
        val entity = ConversationEntity.fromDomain(conversation)
        conversationDao.updateConversation(entity)
    }

    override suspend fun deleteConversation(id: String) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
        if (supabaseClient.isConfigured) {
            runCatching {
                supabaseClient.delete("conversations", "id=eq.$id")
            }
        }
    }

    override suspend fun clearAllConversations() {
        conversationDao.clearAll()
    }

    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMessagesList(conversationId: String): List<Message> {
        return messageDao.getMessagesList(conversationId).map { it.toDomain() }
    }

    override suspend fun saveMessage(message: Message) {
        val entity = MessageEntity.fromDomain(message)
        messageDao.insertMessage(entity)

        // Update conversation summary
        conversationDao.getConversationById(message.conversationId)?.let { conv ->
            val updated = conv.copy(
                lastMessagePreview = message.content.take(60),
                messageCount = conv.messageCount + 1,
                updatedAt = System.currentTimeMillis()
            )
            conversationDao.updateConversation(updated)
        }

        if (supabaseClient.isConfigured) {
            runCatching {
                val json = buildJsonObject {
                    put("id", message.id)
                    put("conversation_id", message.conversationId)
                    put("role", message.role.name.lowercase())
                    put("content", message.content)
                    put("tokens_used", message.tokensUsed)
                }.toString()
                supabaseClient.post("messages", json)
            }
        }
    }

    override suspend fun syncWithCloud(): Result<Unit> {
        return Result.success(Unit)
    }
}

