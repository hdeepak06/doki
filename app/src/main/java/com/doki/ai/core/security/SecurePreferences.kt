package com.doki.ai.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doki_preferences")

class SecurePreferences(private val context: Context) {

    private object Keys {
        val MODEL_IDENTIFIER = stringPreferencesKey("model_identifier")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val AUTO_STOP_SPEECH = booleanPreferencesKey("auto_stop_speech")
        val INTERRUPT_ON_SPEECH = booleanPreferencesKey("interrupt_on_speech")
        val STORE_AUDIO_LOGS = booleanPreferencesKey("store_audio_logs")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val PICOVOICE_ACCESS_KEY = stringPreferencesKey("picovoice_access_key")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val SUPABASE_ANON_KEY = stringPreferencesKey("supabase_anon_key")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<AssistantSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            AssistantSettings(
                modelIdentifier = prefs[Keys.MODEL_IDENTIFIER] ?: "gemini-1.5-flash",
                wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: true,
                wakeWordSensitivity = prefs[Keys.WAKE_WORD_SENSITIVITY] ?: 0.5f,
                preferredLanguage = Language.fromCode(prefs[Keys.PREFERRED_LANGUAGE] ?: "en-US"),
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.0f,
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.0f,
                autoStopSpeech = prefs[Keys.AUTO_STOP_SPEECH] ?: true,
                interruptOnSpeech = prefs[Keys.INTERRUPT_ON_SPEECH] ?: true,
                storeAudioLogs = prefs[Keys.STORE_AUDIO_LOGS] ?: false,
                hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
                porcupineAccessKey = prefs[Keys.PICOVOICE_ACCESS_KEY] ?: "",
                geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
                supabaseUrl = prefs[Keys.SUPABASE_URL] ?: "",
                supabaseAnonKey = prefs[Keys.SUPABASE_ANON_KEY] ?: ""
            )
        }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateSettings(settings: AssistantSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MODEL_IDENTIFIER] = settings.modelIdentifier
            prefs[Keys.WAKE_WORD_ENABLED] = settings.wakeWordEnabled
            prefs[Keys.WAKE_WORD_SENSITIVITY] = settings.wakeWordSensitivity
            prefs[Keys.PREFERRED_LANGUAGE] = settings.preferredLanguage.code
            prefs[Keys.VOICE_SPEED] = settings.voiceSpeed
            prefs[Keys.VOICE_PITCH] = settings.voicePitch
            prefs[Keys.AUTO_STOP_SPEECH] = settings.autoStopSpeech
            prefs[Keys.INTERRUPT_ON_SPEECH] = settings.interruptOnSpeech
            prefs[Keys.STORE_AUDIO_LOGS] = settings.storeAudioLogs
            prefs[Keys.HAPTIC_FEEDBACK] = settings.hapticFeedback
            if (settings.porcupineAccessKey.isNotBlank()) {
                prefs[Keys.PICOVOICE_ACCESS_KEY] = settings.porcupineAccessKey
            }
            if (settings.geminiApiKey.isNotBlank()) {
                prefs[Keys.GEMINI_API_KEY] = settings.geminiApiKey
            }
            prefs[Keys.SUPABASE_URL] = settings.supabaseUrl
            prefs[Keys.SUPABASE_ANON_KEY] = settings.supabaseAnonKey
        }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[Keys.GEMINI_API_KEY] = key }
    }

    suspend fun setPorcupineAccessKey(key: String) {
        context.dataStore.edit { prefs -> prefs[Keys.PICOVOICE_ACCESS_KEY] = key }
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { prefs -> prefs[Keys.PREFERRED_LANGUAGE] = language.code }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.WAKE_WORD_ENABLED] = enabled }
    }
}

