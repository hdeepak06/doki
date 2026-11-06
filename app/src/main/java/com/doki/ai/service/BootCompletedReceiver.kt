package com.doki.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.doki.ai.DokiApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device boot completed, checking DOKI wake-word settings...")
            val app = context.applicationContext as DokiApplication
            CoroutineScope(Dispatchers.IO).launch {
                val settings = app.container.settingsRepository.settingsFlow.first()
                if (settings.wakeWordEnabled && settings.porcupineAccessKey.isNotBlank()) {
                    Log.d("BootCompletedReceiver", "Starting DOKI WakeWordService on boot...")
                    WakeWordService.start(context)
                }
            }
        }
    }
}

