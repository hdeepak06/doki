package com.doki.ai.service

import android.content.Intent
import android.speech.RecognitionService

class DokiRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // Required for voice interaction recognition binding
    }

    override fun onCancel(listener: Callback?) {
    }

    override fun onStopListening(listener: Callback?) {
    }
}

