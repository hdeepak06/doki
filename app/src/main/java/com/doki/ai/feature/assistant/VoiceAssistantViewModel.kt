package com.doki.ai.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.core.ai.AiResponse
import com.doki.ai.core.ai.GeminiAiService
import com.doki.ai.core.ai.tools.ToolExecutionResult
import com.doki.ai.core.voice.DokiSpeechRecognizer
import com.doki.ai.core.voice.DokiTextToSpeech
import com.doki.ai.core.voice.VoiceState
import com.doki.ai.domain.model.Conversation
import com.doki.ai.domain.model.Language
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

class VoiceAssistantViewModel(
    private val speechRecognizer: DokiSpeechRecognizer,
    private val textToSpeech: DokiTextToSpeech,
    private val geminiAiService: GeminiAiService,
    private val memoryRepository: MemoryRepository,
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _assistantReply = MutableStateFlow("")
    val assistantReply: StateFlow<String> = _assistantReply.asStateFlow()

    private val _activeToolResult = MutableStateFlow<ToolExecutionResult?>(null)
    val activeToolResult: StateFlow<ToolExecutionResult?> = _activeToolResult.asStateFlow()

    val audioLevel: StateFlow<Float> = speechRecognizer.rmsLevel

    private var currentConversationId = UUID.randomUUID().toString()
    private val conversationMessages = mutableListOf<Message>()

    init {
        observeSpeechEvents()
        observeTtsEvents()
    }

    private fun observeSpeechEvents() {
        viewModelScope.launch {
            speechRecognizer.speechEvents.collect { event ->
                when (event) {
                    is DokiSpeechRecognizer.SpeechEvent.Ready -> {
                        _voiceState.value = VoiceState.LISTENING
                    }
                    is DokiSpeechRecognizer.SpeechEvent.Recognized -> {
                        _transcription.value = event.text
                        processUserSpeech(event.text, event.language)
                    }
                    is DokiSpeechRecognizer.SpeechEvent.Error -> {
                        _voiceState.value = VoiceState.ERROR
                        _assistantReply.value = event.message
                    }
                    is DokiSpeechRecognizer.SpeechEvent.EndOfSpeech -> {
                        _voiceState.value = VoiceState.THINKING
                    }
                }
            }
        }

        viewModelScope.launch {
            speechRecognizer.partialResults.collect { partial ->
                if (_voiceState.value == VoiceState.LISTENING && partial.isNotBlank()) {
                    _transcription.value = partial
                }
            }
        }
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            textToSpeech.ttsEvents.collect { event ->
                when (event) {
                    is DokiTextToSpeech.TtsEvent.Started -> {
                        _voiceState.value = VoiceState.SPEAKING
                    }
                    is DokiTextToSpeech.TtsEvent.Completed -> {
                        _voiceState.value = VoiceState.IDLE
                    }
                    is DokiTextToSpeech.TtsEvent.Error -> {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            }
        }
    }

    fun startListening() {
        textToSpeech.stop()
        _voiceState.value = VoiceState.LISTENING
        _transcription.value = ""
        _assistantReply.value = ""
        _activeToolResult.value = null

        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            speechRecognizer.startListening(settings.preferredLanguage)
        }
    }

    fun stopSpeakingOrListening() {
        if (_voiceState.value == VoiceState.LISTENING) {
            speechRecognizer.stopListening()
            _voiceState.value = VoiceState.IDLE
        } else if (_voiceState.value == VoiceState.SPEAKING) {
            textToSpeech.stop()
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun processUserSpeech(userText: String, detectedLang: Language) {
        viewModelScope.launch {
            _voiceState.value = VoiceState.THINKING
            val settings = settingsRepository.settingsFlow.first()
            val memories = memoryRepository.searchMemories("")

            // Record user message
            val userMsg = Message(
                conversationId = currentConversationId,
                role = MessageRole.USER,
                content = userText
            )
            conversationMessages.add(userMsg)
            conversationRepository.saveMessage(userMsg)

            val result = geminiAiService.generateResponse(
                apiKey = settings.geminiApiKey,
                modelName = settings.modelIdentifier,
                history = conversationMessages,
                userPrompt = userText,
                userMemories = memories,
                preferredLanguage = settings.preferredLanguage
            )

            result.fold(
                onSuccess = { response ->
                    _assistantReply.value = response.text
                    _activeToolResult.value = response.executedTool

                    val assistantMsg = Message(
                        conversationId = currentConversationId,
                        role = MessageRole.ASSISTANT,
                        content = response.text
                    )
                    conversationMessages.add(assistantMsg)
                    conversationRepository.saveMessage(assistantMsg)

                    // Speak the response in matching language
                    val speakLang = response.detectedLanguage
                    _voiceState.value = VoiceState.SPEAKING
                    textToSpeech.speak(
                        text = response.text,
                        language = speakLang,
                        pitch = settings.voicePitch,
                        speechRate = settings.voiceSpeed
                    )
                },
                onFailure = { error ->
                    _voiceState.value = VoiceState.ERROR
                    val errMsg = "Sorry, I had trouble connecting: ${error.localizedMessage ?: "Unknown error"}"
                    _assistantReply.value = errMsg
                    textToSpeech.speak(errMsg, settings.preferredLanguage)
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.stopListening()
        textToSpeech.stop()
    }
}

