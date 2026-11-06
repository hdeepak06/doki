package com.doki.ai.core.voice

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class PorcupineWakeWordManager(
    private val context: Context
) {
    private val tag = "PorcupineWakeWord"
    private var porcupineManager: PorcupineManager? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPushToTalkFallback = MutableStateFlow(true)
    val isPushToTalkFallback: StateFlow<Boolean> = _isPushToTalkFallback.asStateFlow()

    private val _statusMessage = MutableStateFlow("Wake-word not initialized")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _wakeWordDetected = MutableSharedFlow<Unit>()
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected.asSharedFlow()

    private val callback = PorcupineManagerCallback { keywordIndex ->
        Log.d(tag, "Wake word detected! Keyword index: $keywordIndex")
        _wakeWordDetected.tryEmit(Unit)
    }

    /**
     * Initializes the Picovoice Porcupine engine.
     * Loads custom "Hey DOKI" .ppn model from assets if valid, or falls back to built-in keyword.
     * If AccessKey is missing or invalid, sets push-to-talk fallback.
     */
    fun initialize(accessKey: String, sensitivity: Float = 0.5f): Boolean {
        release()

        if (accessKey.isBlank()) {
            _isPushToTalkFallback.value = true
            _statusMessage.value = "Picovoice AccessKey missing. Push-to-Talk active."
            Log.w(tag, "Porcupine AccessKey is empty; operating in Push-to-Talk fallback.")
            return false
        }

        try {
            val customModelPath = copyAssetToInternalStorage("hey_doki_android.ppn")
            val builder = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setSensitivity(sensitivity.coerceIn(0.0f, 1.0f))

            if (customModelPath != null && isValidPpnFile(customModelPath)) {
                Log.d(tag, "Using custom Hey DOKI wake-word model from $customModelPath")
                builder.setKeywordPath(customModelPath)
            } else {
                Log.w(tag, "Custom Hey DOKI model unavailable/invalid; falling back to PORCUPINE built-in keyword")
                builder.setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
            }

            porcupineManager = builder.build(context, callback)
            _isPushToTalkFallback.value = false
            _statusMessage.value = "Hey DOKI wake word ready"
            Log.d(tag, "Porcupine initialized successfully")
            return true
        } catch (e: PorcupineException) {
            Log.e(tag, "Porcupine initialization failed: ${e.message}", e)
            _isPushToTalkFallback.value = true
            _statusMessage.value = "Wake-word error: ${e.message}. Using Push-to-Talk."
            return false
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error initializing Porcupine", e)
            _isPushToTalkFallback.value = true
            _statusMessage.value = "Wake-word disabled. Using Push-to-Talk."
            return false
        }
    }

    fun startListening(): Boolean {
        if (porcupineManager == null) {
            Log.w(tag, "Cannot start: PorcupineManager is null (fallback mode)")
            return false
        }
        if (_isListening.value) return true

        return try {
            porcupineManager?.start()
            _isListening.value = true
            _statusMessage.value = "Listening for \"Hey DOKI\"..."
            Log.d(tag, "Porcupine audio loop started")
            true
        } catch (e: Exception) {
            Log.e(tag, "Error starting Porcupine listening loop", e)
            _isListening.value = false
            _statusMessage.value = "Microphone error during wake-word listening"
            false
        }
    }

    fun stopListening() {
        if (!_isListening.value) return
        try {
            porcupineManager?.stop()
            _isListening.value = false
            _statusMessage.value = "Wake-word listening paused"
            Log.d(tag, "Porcupine audio loop stopped")
        } catch (e: Exception) {
            Log.w(tag, "Error stopping Porcupine manager", e)
        }
    }

    fun release() {
        stopListening()
        try {
            porcupineManager?.delete()
        } catch (e: Exception) {
            Log.w(tag, "Error releasing Porcupine manager", e)
        }
        porcupineManager = null
        _isListening.value = false
    }

    private fun copyAssetToInternalStorage(filename: String): String? {
        return try {
            val file = File(context.filesDir, filename)
            context.assets.open(filename).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.d(tag, "Asset $filename not found in APK assets", e)
            null
        }
    }

    private fun isValidPpnFile(path: String): Boolean {
        val file = File(path)
        // Ensure file exists and is larger than a simple text notice (compiled PPN models are binary > 5KB)
        return file.exists() && file.length() > 5000
    }
}

