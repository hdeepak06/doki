package com.doki.ai.data.repository

import com.doki.ai.core.security.SecurePreferences
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language
import com.doki.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val securePreferences: SecurePreferences
) : SettingsRepository {

    override val settingsFlow: Flow<AssistantSettings> = securePreferences.settingsFlow
    override val isOnboardingCompleted: Flow<Boolean> = securePreferences.isOnboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        securePreferences.setOnboardingCompleted(completed)
    }

    override suspend fun updateSettings(settings: AssistantSettings) {
        securePreferences.updateSettings(settings)
    }

    override suspend fun setGeminiApiKey(key: String) {
        securePreferences.setGeminiApiKey(key)
    }

    override suspend fun setPorcupineAccessKey(key: String) {
        securePreferences.setPorcupineAccessKey(key)
    }

    override suspend fun setLanguage(language: Language) {
        securePreferences.setLanguage(language)
    }

    override suspend fun setWakeWordEnabled(enabled: Boolean) {
        securePreferences.setWakeWordEnabled(enabled)
    }
}

