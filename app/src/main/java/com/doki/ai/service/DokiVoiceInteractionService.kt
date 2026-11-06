package com.doki.ai.service

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log

class DokiVoiceInteractionService : VoiceInteractionService() {
    private val tag = "DokiVoiceService"

    override fun onReady() {
        super.onReady()
        Log.d(tag, "DokiVoiceInteractionService is ready and bound as assistant")
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d(tag, "DokiVoiceInteractionService shutdown")
    }
}

