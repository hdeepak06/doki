package com.doki.ai.core.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.doki.ai.domain.model.Language
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class DokiTextToSpeech(
    private val context: Context
) : TextToSpeech.OnInitListener {

    private val tag = "DokiTextToSpeech"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsEvents = MutableSharedFlow<TtsEvent>()
    val ttsEvents: SharedFlow<TtsEvent> = _ttsEvents.asSharedFlow()

    sealed class TtsEvent {
        data class Started(val utteranceId: String) : TtsEvent()
        data class Completed(val utteranceId: String) : TtsEvent()
        data class Error(val utteranceId: String, val message: String) : TtsEvent()
    }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _ttsEvents.tryEmit(TtsEvent.Started(utteranceId.orEmpty()))
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _ttsEvents.tryEmit(TtsEvent.Completed(utteranceId.orEmpty()))
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _ttsEvents.tryEmit(TtsEvent.Error(utteranceId.orEmpty(), "TTS playback error"))
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _ttsEvents.tryEmit(TtsEvent.Error(utteranceId.orEmpty(), "TTS error code: $errorCode"))
                }
            })
            Log.d(tag, "TextToSpeech successfully initialized")
        } else {
            Log.e(tag, "Failed to initialize TextToSpeech (status: $status)")
        }
    }

    fun speak(
        text: String,
        language: Language = Language.ENGLISH,
        pitch: Float = 1.0f,
        speechRate: Float = 1.0f
    ) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready yet")
            return
        }

        // Interrupt previous speech
        stop()

        val targetLocale = language.locale
        val langResult = tts?.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(tag, "Locale ${targetLocale.displayName} not supported, falling back to US English")
            tts?.setLanguage(Locale.US)
        }

        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        if (tts != null && _isSpeaking.value) {
            tts?.stop()
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

