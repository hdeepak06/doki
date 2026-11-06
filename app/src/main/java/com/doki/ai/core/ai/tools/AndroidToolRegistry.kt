package com.doki.ai.core.ai.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.doki.ai.domain.model.Memory
import com.doki.ai.domain.model.Reminder
import com.doki.ai.domain.repository.MemoryRepository
import com.doki.ai.domain.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class AndroidToolRegistry(
    private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val reminderRepository: ReminderRepository
) {
    private val tag = "AndroidToolRegistry"
    private var isFlashlightOn = false

    val toolDefinitions: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "open_app",
            description = "Opens an installed Android application on the device by app name or package.",
            parameters = listOf(
                ToolParameter("app_name", "string", "The name of the app to launch, e.g. 'YouTube', 'Spotify', 'WhatsApp'.")
            )
        ),
        ToolDefinition(
            name = "make_call",
            description = "Initiates a phone call to a given phone number or contact name. Requires user confirmation for security.",
            parameters = listOf(
                ToolParameter("phone_number", "string", "The recipient's phone number or contact name.")
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "send_sms",
            description = "Sends an SMS text message to a specified recipient. Requires user confirmation.",
            parameters = listOf(
                ToolParameter("phone_number", "string", "The recipient's phone number."),
                ToolParameter("message", "string", "The message text to send.")
            ),
            requiresConfirmation = true
        ),
        ToolDefinition(
            name = "create_alarm",
            description = "Creates a system clock alarm for a specified hour and minute.",
            parameters = listOf(
                ToolParameter("hour", "integer", "Hour in 24-hour format (0-23)."),
                ToolParameter("minute", "integer", "Minute of the hour (0-59)."),
                ToolParameter("label", "string", "Optional label or description for the alarm.", required = false)
            )
        ),
        ToolDefinition(
            name = "create_reminder",
            description = "Schedules a personal reminder with a due time and optional notes.",
            parameters = listOf(
                ToolParameter("title", "string", "The title or subject of the reminder."),
                ToolParameter("minutes_from_now", "integer", "How many minutes from now to alert the user.", required = false),
                ToolParameter("notes", "string", "Additional details or notes.", required = false)
            )
        ),
        ToolDefinition(
            name = "toggle_flashlight",
            description = "Toggles the device camera flashlight / torch on or off.",
            parameters = listOf(
                ToolParameter("state", "boolean", "True to turn on flashlight, False to turn it off.")
            )
        ),
        ToolDefinition(
            name = "get_battery",
            description = "Returns current battery percentage, charging state, and power source.",
            parameters = emptyList()
        ),
        ToolDefinition(
            name = "get_device_info",
            description = "Retrieves device hardware specifications, Android OS version, storage and memory usage.",
            parameters = emptyList()
        ),
        ToolDefinition(
            name = "open_navigation",
            description = "Opens GPS navigation or Google Maps directed to a specific destination or address.",
            parameters = listOf(
                ToolParameter("destination", "string", "Address or landmark to navigate to.")
            )
        ),
        ToolDefinition(
            name = "search_web",
            description = "Performs a web search using the user's default browser.",
            parameters = listOf(
                ToolParameter("query", "string", "The web search query.")
            )
        ),
        ToolDefinition(
            name = "open_settings",
            description = "Opens Android system settings or a specific setting page (wifi, bluetooth, display, sound).",
            parameters = listOf(
                ToolParameter("setting_type", "string", "Optional setting category: 'wifi', 'bluetooth', 'display', 'sound', 'all'.", required = false)
            )
        ),
        ToolDefinition(
            name = "save_memory",
            description = "Stores a long-term personal memory or fact about the user (e.g. coffee preference, pet name).",
            parameters = listOf(
                ToolParameter("key", "string", "Key identifier, e.g. 'favorite_coffee'."),
                ToolParameter("value", "string", "The information to remember, e.g. 'Oat milk flat white'."),
                ToolParameter("category", "string", "Category: 'preference', 'fact', 'contact', 'routine', 'health'.", required = false)
            )
        ),
        ToolDefinition(
            name = "get_memory",
            description = "Retrieves previously saved personal memories or facts about the user.",
            parameters = listOf(
                ToolParameter("query", "string", "Search query or keyword to find stored memories.")
            )
        ),
        ToolDefinition(
            name = "send_whatsapp",
            description = "Opens WhatsApp with a prefilled message to a phone number or contact.",
            parameters = listOf(
                ToolParameter("phone_number", "string", "Phone number with international country code (no '+' or leading zeros).", required = false),
                ToolParameter("message", "string", "Message text to send.")
            )
        )
    )

    suspend fun executeTool(
        name: String,
        arguments: Map<String, Any?>,
        confirmed: Boolean = false
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val definition = toolDefinitions.firstOrNull { it.name == name }
            ?: return@withContext ToolExecutionResult.Error(
                toolName = name,
                error = "ToolNotFound",
                explanation = "Tool '$name' is not registered in DOKI AndroidToolRegistry."
            )

        if (definition.requiresConfirmation && !confirmed) {
            val stringArgs = arguments.mapValues { it.value?.toString() ?: "" }
            val prompt = when (name) {
                "make_call" -> "Are you sure you want to call ${arguments["phone_number"]}?"
                "send_sms" -> "Do you want to send this SMS to ${arguments["phone_number"]}: \"${arguments["message"]}\"?"
                else -> "Please confirm you want to proceed with $name."
            }
            return@withContext ToolExecutionResult.RequiresConfirmation(
                toolName = name,
                confirmationPrompt = prompt,
                pendingArgs = stringArgs
            )
        }

        try {
            when (name) {
                "open_app" -> executeOpenApp(arguments["app_name"]?.toString() ?: "")
                "make_call" -> executeMakeCall(arguments["phone_number"]?.toString() ?: "")
                "send_sms" -> executeSendSms(
                    phoneNumber = arguments["phone_number"]?.toString() ?: "",
                    message = arguments["message"]?.toString() ?: ""
                )
                "create_alarm" -> executeCreateAlarm(
                    hour = (arguments["hour"] as? Number)?.toInt() ?: 7,
                    minute = (arguments["minute"] as? Number)?.toInt() ?: 0,
                    label = arguments["label"]?.toString() ?: "DOKI Alarm"
                )
                "create_reminder" -> executeCreateReminder(
                    title = arguments["title"]?.toString() ?: "Reminder",
                    minutesFromNow = (arguments["minutes_from_now"] as? Number)?.toInt() ?: 30,
                    notes = arguments["notes"]?.toString()
                )
                "toggle_flashlight" -> executeToggleFlashlight(
                    state = arguments["state"] as? Boolean ?: !isFlashlightOn
                )
                "get_battery" -> executeGetBattery()
                "get_device_info" -> executeGetDeviceInfo()
                "open_navigation" -> executeOpenNavigation(arguments["destination"]?.toString() ?: "")
                "search_web" -> executeSearchWeb(arguments["query"]?.toString() ?: "")
                "open_settings" -> executeOpenSettings(arguments["setting_type"]?.toString() ?: "all")
                "save_memory" -> executeSaveMemory(
                    key = arguments["key"]?.toString() ?: "",
                    value = arguments["value"]?.toString() ?: "",
                    category = arguments["category"]?.toString() ?: "preference"
                )
                "get_memory" -> executeGetMemory(arguments["query"]?.toString() ?: "")
                "send_whatsapp" -> executeSendWhatsapp(
                    phoneNumber = arguments["phone_number"]?.toString(),
                    message = arguments["message"]?.toString() ?: ""
                )
                else -> ToolExecutionResult.Error(name, "Unsupported", "Tool '$name' not implemented.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error executing tool $name", e)
            ToolExecutionResult.Error(
                toolName = name,
                error = e.javaClass.simpleName,
                explanation = "Failed to execute $name: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun executeOpenApp(appName: String): ToolExecutionResult {
        if (appName.isBlank()) {
            return ToolExecutionResult.Error("open_app", "EmptyArgument", "App name cannot be empty.")
        }
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Try exact match or fuzzy match on app label
        val matched = packages.firstOrNull { appInfo ->
            val label = pm.getApplicationLabel(appInfo).toString()
            label.equals(appName, ignoreCase = true) || label.contains(appName, ignoreCase = true)
        } ?: packages.firstOrNull { it.packageName.contains(appName, ignoreCase = true) }

        return if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = pm.getApplicationLabel(matched).toString()
                ToolExecutionResult.Success(
                    toolName = "open_app",
                    output = "Launched app: $label (${matched.packageName})",
                    userFeedback = "Opening $label for you."
                )
            } else {
                ToolExecutionResult.Error("open_app", "NoLauncher", "Cannot find a launcher intent for $appName.")
            }
        } else {
            ToolExecutionResult.Error("open_app", "NotFound", "Application '$appName' is not installed on this device.")
        }
    }

    private fun executeMakeCall(phoneNumber: String): ToolExecutionResult {
        if (phoneNumber.isBlank()) {
            return ToolExecutionResult.Error("make_call", "InvalidNumber", "Phone number is invalid or empty.")
        }
        val hasCallPerm = context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val intent = if (hasCallPerm) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolExecutionResult.Success(
            toolName = "make_call",
            output = "Calling $phoneNumber",
            userFeedback = "Calling $phoneNumber now."
        )
    }

    private fun executeSendSms(phoneNumber: String, message: String): ToolExecutionResult {
        if (phoneNumber.isBlank() || message.isBlank()) {
            return ToolExecutionResult.Error("send_sms", "InvalidArguments", "Phone number and message must not be blank.")
        }
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)
        return ToolExecutionResult.Success(
            toolName = "send_sms",
            output = "Drafted SMS to $phoneNumber: $message",
            userFeedback = "Prepared your SMS to $phoneNumber."
        )
    }

    private fun executeCreateAlarm(hour: Int, minute: Int, label: String): ToolExecutionResult {
        if (hour !in 0..23 || minute !in 0..59) {
            return ToolExecutionResult.Error("create_alarm", "InvalidTime", "Hour must be 0-23 and minute 0-59.")
        }
        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(alarmIntent)
        val formattedTime = String.format("%02d:%02d", hour, minute)
        return ToolExecutionResult.Success(
            toolName = "create_alarm",
            output = "Alarm set for $formattedTime with label '$label'",
            userFeedback = "I've set your alarm for $formattedTime."
        )
    }

    private suspend fun executeCreateReminder(title: String, minutesFromNow: Int, notes: String?): ToolExecutionResult {
        val dueTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
        val reminder = Reminder(
            title = title,
            notes = notes,
            dueTime = dueTime
        )
        reminderRepository.saveReminder(reminder)
        return ToolExecutionResult.Success(
            toolName = "create_reminder",
            output = "Reminder '$title' saved, due in $minutesFromNow minutes.",
            userFeedback = "Reminder saved: '$title' in $minutesFromNow minutes."
        )
    }

    private fun executeToggleFlashlight(state: Boolean): ToolExecutionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ToolExecutionResult.Error("toggle_flashlight", "NoCameraManager", "Camera service not available.")
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ToolExecutionResult.Error("toggle_flashlight", "NoFlash", "No camera with flash found on this device.")

            cameraManager.setTorchMode(cameraId, state)
            isFlashlightOn = state
            val status = if (state) "turned on" else "turned off"
            ToolExecutionResult.Success(
                toolName = "toggle_flashlight",
                output = "Flashlight is now $status",
                userFeedback = "Flashlight $status."
            )
        } catch (e: CameraAccessException) {
            ToolExecutionResult.Error("toggle_flashlight", "CameraAccessException", e.message ?: "Failed to access torch.")
        }
    }

    private fun executeGetBattery(): ToolExecutionResult {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Power"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not plugged in"
        }

        val text = "Battery is at $batteryPct% (${if (isCharging) "Charging via $plugType" else "Discharging"})."
        return ToolExecutionResult.Success(
            toolName = "get_battery",
            output = "Level: $batteryPct%, Charging: $isCharging, Plug: $plugType",
            userFeedback = text
        )
    }

    private fun executeGetDeviceInfo(): ToolExecutionResult {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT

        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeGb = freeBytes / (1024 * 1024 * 1024)
        val totalGb = totalBytes / (1024 * 1024 * 1024)

        // Memory
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)
        val freeRamGb = memInfo.availMem / (1024 * 1024 * 1024)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024)

        val infoString = "$manufacturer $model running Android $androidVersion (API $sdkInt). " +
                "Storage: ${freeGb}GB free of ${totalGb}GB. RAM: ${freeRamGb}GB free of ${totalRamGb}GB."

        return ToolExecutionResult.Success(
            toolName = "get_device_info",
            output = infoString,
            userFeedback = infoString
        )
    }

    private fun executeOpenNavigation(destination: String): ToolExecutionResult {
        if (destination.isBlank()) {
            return ToolExecutionResult.Error("open_navigation", "EmptyDestination", "Destination cannot be blank.")
        }
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            ToolExecutionResult.Success(
                toolName = "open_navigation",
                output = "Navigating to $destination",
                userFeedback = "Navigating to $destination with Google Maps."
            )
        } else {
            // Fallback to geo URI
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(destination)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            ToolExecutionResult.Success(
                toolName = "open_navigation",
                output = "Searching maps for $destination",
                userFeedback = "Showing directions to $destination."
            )
        }
    }

    private fun executeSearchWeb(query: String): ToolExecutionResult {
        if (query.isBlank()) {
            return ToolExecutionResult.Error("search_web", "EmptyQuery", "Search query cannot be empty.")
        }
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ToolExecutionResult.Success(
            toolName = "search_web",
            output = "Searching web for '$query'",
            userFeedback = "Searching the web for $query."
        )
    }

    private fun executeOpenSettings(settingType: String): ToolExecutionResult {
        val action = when (settingType.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ToolExecutionResult.Success(
            toolName = "open_settings",
            output = "Opened settings: $settingType",
            userFeedback = "Opening $settingType settings."
        )
    }

    private suspend fun executeSaveMemory(key: String, value: String, category: String): ToolExecutionResult {
        if (key.isBlank() || value.isBlank()) {
            return ToolExecutionResult.Error("save_memory", "InvalidArguments", "Key and value are required.")
        }
        val memory = Memory(
            key = key.trim(),
            value = value.trim(),
            category = category.trim()
        )
        memoryRepository.saveMemory(memory)
        return ToolExecutionResult.Success(
            toolName = "save_memory",
            output = "Memory saved: [$key] = $value",
            userFeedback = "Got it! I will remember that $value."
        )
    }

    private suspend fun executeGetMemory(query: String): ToolExecutionResult {
        val memories = if (query.isBlank()) {
            memoryRepository.searchMemories("")
        } else {
            memoryRepository.searchMemories(query)
        }
        val result = if (memories.isEmpty()) {
            "No personal memories found matching '$query'."
        } else {
            memories.joinToString("\n") { "• ${it.key}: ${it.value}" }
        }
        return ToolExecutionResult.Success(
            toolName = "get_memory",
            output = result,
            userFeedback = result
        )
    }

    private fun executeSendWhatsapp(phoneNumber: String?, message: String): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val url = if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
                "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            data = Uri.parse(url)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            ToolExecutionResult.Success(
                toolName = "send_whatsapp",
                output = "Opened WhatsApp with message",
                userFeedback = "Opening WhatsApp with your message."
            )
        } else {
            ToolExecutionResult.Error("send_whatsapp", "NotInstalled", "WhatsApp is not installed on this device.")
        }
    }
}

