package com.doki.ai.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language
import com.doki.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedLanguage = MutableStateFlow(Language.ENGLISH)
    val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _picovoiceAccessKey = MutableStateFlow("")
    val picovoiceAccessKey: StateFlow<String> = _picovoiceAccessKey.asStateFlow()

    private val _supabaseUrl = MutableStateFlow("")
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseAnonKey = MutableStateFlow("")
    val supabaseAnonKey: StateFlow<String> = _supabaseAnonKey.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _selectedLanguage.value = settings.preferredLanguage
            _geminiApiKey.value = settings.geminiApiKey
            _picovoiceAccessKey.value = settings.porcupineAccessKey
            _supabaseUrl.value = settings.supabaseUrl
            _supabaseAnonKey.value = settings.supabaseAnonKey
        }
    }

    fun selectLanguage(language: Language) {
        _selectedLanguage.value = language
    }

    fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
    }

    fun updatePicovoiceAccessKey(key: String) {
        _picovoiceAccessKey.value = key
    }

    fun updateSupabaseUrl(url: String) {
        _supabaseUrl.value = url
    }

    fun updateSupabaseAnonKey(key: String) {
        _supabaseAnonKey.value = key
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.settingsFlow.first()
            val updated = currentSettings.copy(
                preferredLanguage = _selectedLanguage.value,
                geminiApiKey = _geminiApiKey.value,
                porcupineAccessKey = _picovoiceAccessKey.value,
                supabaseUrl = _supabaseUrl.value,
                supabaseAnonKey = _supabaseAnonKey.value
            )
            settingsRepository.updateSettings(updated)
            settingsRepository.setOnboardingCompleted(true)
            onSuccess()
        }
    }
}

