package com.doki.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.doki.ai.domain.model.Language
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

class DokiSpeechRecognizer(
    private val context: Context
) {
    private val tag = "DokiSpeechRecognizer"

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()

    private val _speechEvents = MutableSharedFlow<SpeechEvent>()
    val speechEvents: SharedFlow<SpeechEvent> = _speechEvents.asSharedFlow()

    sealed class SpeechEvent {
        data class Recognized(val text: String, val language: Language) : SpeechEvent()
        data class Error(val code: Int, val message: String) : SpeechEvent()
        data object Ready : SpeechEvent()
        data object EndOfSpeech : SpeechEvent()
    }

    private fun initRecognizer() {
        if (speechRecognizer == null) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            } else {
                Log.w(tag, "Speech recognition not available on device")
            }
        }
    }

    fun startListening(language: Language = Language.ENGLISH) {
        if (isListening) {
            stopListening()
        }
        initRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.code)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.code)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            _partialResults.value = ""
        } catch (e: Exception) {
            Log.e(tag, "Error starting speech recognition", e)
            _speechEvents.tryEmit(SpeechEvent.Error(-1, e.message ?: "Failed to start listening"))
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping recognizer", e)
        }
        _rmsLevel.value = 0f
    }

    fun cancel() {
        isListening = false
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(tag, "Error cancelling recognizer", e)
        }
        _rmsLevel.value = 0f
        _partialResults.value = ""
    }

    fun destroy() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _speechEvents.tryEmit(SpeechEvent.Ready)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize RMS dB (typical range -2dB to 10dB) into 0.0 - 1.0
            val normalized = min(1f, max(0f, (rmsdB + 2f) / 12f))
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _rmsLevel.value = 0f
            _speechEvents.tryEmit(SpeechEvent.EndOfSpeech)
        }

        override fun onError(error: Int) {
            isListening = false
            _rmsLevel.value = 0f
            val message = getErrorReason(error)
            Log.d(tag, "SpeechRecognizer error: $error ($message)")
            _speechEvents.tryEmit(SpeechEvent.Error(error, message))
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull().orEmpty().trim()
            if (recognizedText.isNotBlank()) {
                val detectedLanguage = Language.detectFromText(recognizedText)
                _speechEvents.tryEmit(SpeechEvent.Recognized(recognizedText, detectedLanguage))
            } else {
                _speechEvents.tryEmit(SpeechEvent.Error(-1, "No speech recognized"))
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { partial ->
                _partialResults.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun getErrorReason(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
        else -> "Speech recognition error ($errorCode)"
    }
}

