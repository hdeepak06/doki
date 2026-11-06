package com.doki.ai.domain.model

data class AssistantSettings(
    val modelIdentifier: String = "gemini-1.5-flash",
    val wakeWordEnabled: Boolean = true,
    val wakeWordSensitivity: Float = 0.5f,
    val preferredLanguage: Language = Language.ENGLISH,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val autoStopSpeech: Boolean = true,
    val interruptOnSpeech: Boolean = true,
    val storeAudioLogs: Boolean = false,
    val hapticFeedback: Boolean = true,
    val porcupineAccessKey: String = "",
    val geminiApiKey: String = "",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = ""
)

