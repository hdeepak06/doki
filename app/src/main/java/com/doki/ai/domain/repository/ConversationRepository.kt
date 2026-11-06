package com.doki.ai.domain.repository

import com.doki.ai.domain.model.Conversation
import com.doki.ai.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun createConversation(conversation: Conversation)
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteConversation(id: String)
    suspend fun clearAllConversations()

    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun getMessagesList(conversationId: String): List<Message>
    suspend fun saveMessage(message: Message)
    suspend fun syncWithCloud(): Result<Unit>
}

