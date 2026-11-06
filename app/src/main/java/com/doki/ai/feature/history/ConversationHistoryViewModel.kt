package com.doki.ai.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.domain.model.Conversation
import com.doki.ai.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationHistoryViewModel(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = conversationRepository.getAllConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            conversationRepository.clearAllConversations()
        }
    }
}

