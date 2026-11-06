package com.doki.ai.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.core.voice.PorcupineWakeWordManager
import com.doki.ai.core.voice.VoiceState
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.repository.SettingsRepository
import com.doki.ai.service.WakeWordService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val wakeWordManager: PorcupineWakeWordManager
) : ViewModel() {

    val settings: StateFlow<AssistantSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AssistantSettings()
    )

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _textQuery = MutableStateFlow("")
    val textQuery: StateFlow<String> = _textQuery.asStateFlow()

    val isWakeWordListening: StateFlow<Boolean> = wakeWordManager.isListening
    val wakeWordStatus: StateFlow<String> = wakeWordManager.statusMessage
    val isPushToTalk: StateFlow<Boolean> = wakeWordManager.isPushToTalkFallback

    init {
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            while (true) {
                val now = Date()
                _currentTime.value = timeFormat.format(now)
                _currentDate.value = dateFormat.format(now)
                delay(1000)
            }
        }
    }

    fun onTextQueryChange(query: String) {
        _textQuery.value = query
    }

    fun toggleWakeWordService(context: Context) {
        viewModelScope.launch {
            if (isWakeWordListening.value) {
                WakeWordService.stop(context)
            } else {
                WakeWordService.start(context)
            }
        }
    }

    fun getGreeting(): String {
        val calendar = java.util.Calendar.getInstance()
        return when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning. I'm DOKI."
            in 12..16 -> "Good afternoon. I'm DOKI."
            in 17..21 -> "Good evening. I'm DOKI."
            else -> "Hello, I'm DOKI."
        }
    }
}

