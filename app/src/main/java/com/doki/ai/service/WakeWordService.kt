package com.doki.ai.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.doki.ai.DokiApplication
import com.doki.ai.MainActivity
import com.doki.ai.core.voice.PorcupineWakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        const val ACTION_START_LISTENING = "com.doki.ai.ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.doki.ai.ACTION_STOP_LISTENING"
        const val ACTION_RELOAD_SETTINGS = "com.doki.ai.ACTION_RELOAD_SETTINGS"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var wakeWordManager: PorcupineWakeWordManager
    private lateinit var notificationService: AssistantNotificationService
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WakeWordService onCreate")
        val app = application as DokiApplication
        wakeWordManager = app.container.wakeWordManager
        notificationService = AssistantNotificationService(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DokiAI::WakeWordLock").apply {
            setReferenceCounted(false)
        }

        observeWakeWordDetections()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_LISTENING
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_START_LISTENING -> startWakeWordListening()
            ACTION_STOP_LISTENING -> stopWakeWordListening()
            ACTION_RELOAD_SETTINGS -> reloadSettings()
        }

        return START_STICKY
    }

    private fun startWakeWordListening() {
        if (isServiceRunning) return

        val notification = notificationService.buildListeningNotification(true, "Listening for \"Hey DOKI\"...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                AssistantNotificationService.NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
            )
        } else {
            startForeground(AssistantNotificationService.NOTIFICATION_ID, notification)
        }

        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes timeout safety
        isServiceRunning = true

        serviceScope.launch {
            val app = application as DokiApplication
            val settings = app.container.settingsRepository.settingsFlow.first()
            if (settings.wakeWordEnabled && settings.porcupineAccessKey.isNotBlank()) {
                val initSuccess = wakeWordManager.initialize(
                    accessKey = settings.porcupineAccessKey,
                    sensitivity = settings.wakeWordSensitivity
                )
                if (initSuccess) {
                    wakeWordManager.startListening()
                    notificationService.updateNotification(true, "Listening for \"Hey DOKI\"")
                } else {
                    notificationService.updateNotification(false, "Push-to-Talk active (AccessKey error)")
                }
            } else {
                notificationService.updateNotification(false, "Push-to-Talk active (Wake word disabled or key not set)")
            }
        }
    }

    private fun stopWakeWordListening() {
        Log.d(TAG, "Stopping wake-word listening service")
        wakeWordManager.stopListening()
        isServiceRunning = false
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun reloadSettings() {
        serviceScope.launch {
            val app = application as DokiApplication
            val settings = app.container.settingsRepository.settingsFlow.first()
            wakeWordManager.initialize(settings.porcupineAccessKey, settings.wakeWordSensitivity)
            if (isServiceRunning && settings.wakeWordEnabled) {
                wakeWordManager.startListening()
            }
        }
    }

    private fun observeWakeWordDetections() {
        serviceScope.launch {
            wakeWordManager.wakeWordDetected.collect {
                Log.d(TAG, "Wake word detected in background service! Launching Assistant UI...")
                // Launch MainActivity with voice activation flag
                val intent = Intent(this@WakeWordService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_START_VOICE", true)
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WakeWordService onDestroy")
        wakeWordManager.stopListening()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        isServiceRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

