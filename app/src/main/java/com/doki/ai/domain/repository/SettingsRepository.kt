package com.doki.ai.domain.repository

import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AssistantSettings>
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateSettings(settings: AssistantSettings)
    suspend fun setGeminiApiKey(key: String)
    suspend fun setPorcupineAccessKey(key: String)
    suspend fun setLanguage(language: Language)
    suspend fun setWakeWordEnabled(enabled: Boolean)
}

