package com.doki.ai.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.core.voice.DokiTextToSpeech
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language
import com.doki.ai.domain.repository.SettingsRepository
import com.doki.ai.service.WakeWordService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val textToSpeech: DokiTextToSpeech
) : ViewModel() {

    val settings: StateFlow<AssistantSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AssistantSettings()
        )

    fun updateSettings(newSettings: AssistantSettings, context: Context? = null) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
            if (context != null) {
                if (newSettings.wakeWordEnabled && newSettings.porcupineAccessKey.isNotBlank()) {
                    WakeWordService.start(context)
                } else {
                    WakeWordService.stop(context)
                }
            }
        }
    }

    fun testVoice(settings: AssistantSettings) {
        val testText = when (settings.preferredLanguage) {
            Language.KANNADA -> "ನಮಸ್ಕಾರ! ನಾನು ಡೋಕಿ, ನಿಮ್ಮ ವೈಯಕ್ತಿಕ ಸಹಾಯಕ."
            Language.HINDI -> "नमस्ते! मैं डोकी हूँ, आपका व्यक्तिगत सहायक।"
            Language.ENGLISH -> "Hello! I am DOKI, your personal AI assistant."
        }
        textToSpeech.speak(
            text = testText,
            language = settings.preferredLanguage,
            pitch = settings.voicePitch,
            speechRate = settings.voiceSpeed
        )
    }
}

