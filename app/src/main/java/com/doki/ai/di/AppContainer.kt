package com.doki.ai.di

import android.content.Context
import com.doki.ai.core.ai.GeminiAiService
import com.doki.ai.core.ai.tools.AndroidToolRegistry
import com.doki.ai.core.security.SecurePreferences
import com.doki.ai.core.voice.DokiSpeechRecognizer
import com.doki.ai.core.voice.DokiTextToSpeech
import com.doki.ai.core.voice.PorcupineWakeWordManager
import com.doki.ai.data.local.DokiDatabase
import com.doki.ai.data.remote.SupabaseClient
import com.doki.ai.data.repository.CommandRepositoryImpl
import com.doki.ai.data.repository.ConversationRepositoryImpl
import com.doki.ai.data.repository.MemoryRepositoryImpl
import com.doki.ai.data.repository.ReminderRepositoryImpl
import com.doki.ai.data.repository.SettingsRepositoryImpl
import com.doki.ai.domain.repository.CommandRepository
import com.doki.ai.domain.repository.ConversationRepository
import com.doki.ai.domain.repository.MemoryRepository
import com.doki.ai.domain.repository.ReminderRepository
import com.doki.ai.domain.repository.SettingsRepository

interface AppContainer {
    val database: DokiDatabase
    val securePreferences: SecurePreferences
    val supabaseClient: SupabaseClient
    val memoryRepository: MemoryRepository
    val conversationRepository: ConversationRepository
    val reminderRepository: ReminderRepository
    val commandRepository: CommandRepository
    val settingsRepository: SettingsRepository
    val toolRegistry: AndroidToolRegistry
    val geminiAiService: GeminiAiService
    val speechRecognizer: DokiSpeechRecognizer
    val textToSpeech: DokiTextToSpeech
    val wakeWordManager: PorcupineWakeWordManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: DokiDatabase by lazy {
        DokiDatabase.getInstance(context)
    }

    override val securePreferences: SecurePreferences by lazy {
        SecurePreferences(context)
    }

    override val supabaseClient: SupabaseClient by lazy {
        SupabaseClient("", "")
    }

    override val memoryRepository: MemoryRepository by lazy {
        MemoryRepositoryImpl(database.memoryDao(), supabaseClient)
    }

    override val conversationRepository: ConversationRepository by lazy {
        ConversationRepositoryImpl(database.conversationDao(), database.messageDao(), supabaseClient)
    }

    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(database.reminderDao())
    }

    override val commandRepository: CommandRepository by lazy {
        CommandRepositoryImpl(database.customCommandDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(securePreferences)
    }

    override val toolRegistry: AndroidToolRegistry by lazy {
        AndroidToolRegistry(context, memoryRepository, reminderRepository)
    }

    override val geminiAiService: GeminiAiService by lazy {
        GeminiAiService(toolRegistry)
    }

    override val speechRecognizer: DokiSpeechRecognizer by lazy {
        DokiSpeechRecognizer(context)
    }

    override val textToSpeech: DokiTextToSpeech by lazy {
        DokiTextToSpeech(context)
    }

    override val wakeWordManager: PorcupineWakeWordManager by lazy {
        PorcupineWakeWordManager(context)
    }
}

