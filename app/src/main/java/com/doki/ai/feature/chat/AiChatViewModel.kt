package com.doki.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.core.ai.GeminiAiService
import com.doki.ai.core.ai.tools.ToolExecutionResult
import com.doki.ai.domain.model.Conversation
import com.doki.ai.domain.model.Message
import com.doki.ai.domain.model.MessageRole
import com.doki.ai.domain.repository.ConversationRepository
import com.doki.ai.domain.repository.MemoryRepository
import com.doki.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class AiChatViewModel(
    private val conversationRepository: ConversationRepository,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiAiService: GeminiAiService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeToolResult = MutableStateFlow<ToolExecutionResult?>(null)
    val activeToolResult: StateFlow<ToolExecutionResult?> = _activeToolResult.asStateFlow()

    private var conversationId: String = UUID.randomUUID().toString()

    fun loadConversation(id: String, initialPrompt: String? = null) {
        conversationId = if (id == "new") UUID.randomUUID().toString() else id

        viewModelScope.launch {
            if (id != "new") {
                val existing = conversationRepository.getMessagesList(conversationId)
                _messages.value = existing
            } else {
                conversationRepository.createConversation(
                    Conversation(
                        id = conversationId,
                        title = initialPrompt?.take(30) ?: "New Conversation",
                        interactionType = "chat"
                    )
                )
            }

            if (!initialPrompt.isNullOrBlank()) {
                sendMessage(initialPrompt)
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        val userMessage = Message(
            conversationId = conversationId,
            role = MessageRole.USER,
            content = content
        )

        _messages.value = _messages.value + userMessage
        _isGenerating.value = true
        _activeToolResult.value = null

        viewModelScope.launch {
            conversationRepository.saveMessage(userMessage)

            val settings = settingsRepository.settingsFlow.first()
            val memories = memoryRepository.searchMemories("")

            val result = geminiAiService.generateResponse(
                apiKey = settings.geminiApiKey,
                modelName = settings.modelIdentifier,
                history = _messages.value,
                userPrompt = content,
                userMemories = memories,
                preferredLanguage = settings.preferredLanguage
            )

            result.fold(
                onSuccess = { aiResponse ->
                    _activeToolResult.value = aiResponse.executedTool
                    val assistantMessage = Message(
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = aiResponse.text
                    )
                    _messages.value = _messages.value + assistantMessage
                    conversationRepository.saveMessage(assistantMessage)
                },
                onFailure = { error ->
                    val errorMessage = Message(
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = "⚠️ ${error.localizedMessage ?: "Network or API failure."}"
                    )
                    _messages.value = _messages.value + errorMessage
                }
            )

            _isGenerating.value = false
        }
    }
}

