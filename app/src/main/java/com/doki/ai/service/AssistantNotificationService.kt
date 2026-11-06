package com.doki.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.doki.ai.MainActivity
import com.doki.ai.R

class AssistantNotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "doki_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_LISTENING = "com.doki.ai.ACTION_STOP_LISTENING"
        const val ACTION_OPEN_ASSISTANT = "com.doki.ai.ACTION_OPEN_ASSISTANT"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildListeningNotification(isListening: Boolean, statusMessage: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_START_VOICE", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, WakeWordService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isListening) "DOKI is listening" else "DOKI in Standby"
        val text = if (isListening) "Say \"Hey DOKI\" or tap to speak" else statusMessage

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(isListening)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .addAction(
                android.R.drawable.ic_btn_speak_now,
                context.getString(R.string.notification_action_open),
                openPendingIntent
            )
            .build()
    }

    fun updateNotification(isListening: Boolean, message: String) {
        val notification = buildListeningNotification(isListening, message)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

