package com.doki.ai.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isRequired: Boolean = false
)

object PermissionManager {

    fun getAssistantPermissions(): List<PermissionItem> {
        val list = mutableListOf(
            PermissionItem(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone Access",
                description = "Required for speech recognition and \"Hey DOKI\" wake-word detection.",
                isRequired = true
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Notifications",
                    description = "Shows the active listening status and quick controls.",
                    isRequired = false
                )
            )
        }

        list.add(
            PermissionItem(
                permission = Manifest.permission.CALL_PHONE,
                title = "Phone Calls",
                description = "Allows DOKI to dial and place phone calls when you request.",
                isRequired = false
            )
        )

        list.add(
            PermissionItem(
                permission = Manifest.permission.SEND_SMS,
                title = "SMS Messaging",
                description = "Enables DOKI to send text messages upon your voice command.",
                isRequired = false
            )
        )

        list.add(
            PermissionItem(
                permission = Manifest.permission.CAMERA,
                title = "Flashlight / Camera",
                description = "Required to toggle the device flashlight on and off.",
                isRequired = false
            )
        )

        return list
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun areCorePermissionsGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }
}

