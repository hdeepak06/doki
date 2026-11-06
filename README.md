# DOKI AI – Personal Android AI Assistant

![DOKI AI Header](https://img.shields.io/badge/Platform-Android%2026%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%201.9.24-blue)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material3)-cyan)
![AI](https://img.shields.io/badge/AI-Google%20Gemini%20Flash%20%2F%20Pro-violet)
![Wake Word](https://img.shields.io/badge/Wake%20Word-Picovoice%20Porcupine-orange)
![Database](https://img.shields.io/badge/Cloud%20Database-Supabase%20PostgreSQL-darkgreen)

**DOKI** is a production-grade, native Android personal AI assistant designed with Alexa-like continuous voice wake-word capabilities, Google Gemini AI intelligence, multilingual conversational understanding (English, Kannada, and Hindi), secure on-device Android hardware controls, and a persistent memory store powered by Room and Supabase.

---

## 🌟 Key Capabilities

### 1. Futuristic Cybernetic Interface
- Deep Space Black (`#060913`) and Deep Navy (`#0A0F1D`) background palette.
- **Dynamic AI Orb**: Custom Compose Canvas shader animations with 5 state transitions:
  1. **Idle**: Breathing glow, slow orbital rotation, subtle ambient shimmer.
  2. **Listening**: Audio-reactive amplitude pulse with concentric expanding cyan shockwaves.
  3. **Thinking**: Orbiting swirling dual rings (cyan & violet) with high-frequency shimmer.
  4. **Speaking**: Harmonic audio frequency ripples dancing with violet dominance.
  5. **Error**: Warning amber/crimson tint with high-frequency jitter vibration.
- **Real-Time Sound Wave**: Animated audio waveform bars reflecting actual microphone decibels.
- **Glowing Mic Button**: Haptic-responsive tactile button with glowing aura.

### 2. Google Gemini AI Engine
- Multi-turn conversational context with memory injection.
- Multilingual intelligence supporting **English**, **Kannada (ಕನ್ನಡ)**, and **Hindi (हिन्दी)** with automatic language detection and matching responses.
- Streaming response generation (`generateContentStream`) for rapid speech synthesis.
- Configurable model identifiers (`gemini-1.5-flash`, `gemini-1.5-pro`, `gemini-2.0-flash`).
- Production security: No hardcoded secrets; API keys read from encrypted DataStore.

### 3. Picovoice Porcupine Wake-Word Engine
- Continuous local wake-word detection for **"Hey DOKI"**.
- Zero cloud transmission during wake-word listening: 100% on-device edge ML.
- Background Android Foreground Service (`WakeWordService`) typed as `microphone` (Android 14+ / API 34+ compliant).
- Persistent status notification with one-tap controls.
- Push-to-Talk fallback if the Picovoice AccessKey or custom model is not configured.
- Clean audio focus handoff to prevent microphone conflicts with speech recognition.

### 4. Secure Android Device Control (`AndroidToolRegistry`)
Executes real device actions via native Android APIs and system Intents:
- `open_app`: Queries PackageManager and launches any installed app by label or package name.
- `make_call`: Dials or places phone calls with sensitive action confirmation.
- `send_sms`: Sends SMS with contact and message validation after user confirmation.
- `create_alarm`: Sets clock alarms via `AlarmClock.ACTION_SET_ALARM`.
- `create_reminder`: Schedules reminders in Room DB and system calendar/alarms.
- `toggle_flashlight`: CameraManager torch mode toggle.
- `get_battery`: Real-time battery percentage, charging state, and charger type.
- `get_device_info`: Hardware model, manufacturer, OS version, RAM, and storage stats.
- `open_navigation`: Turn-by-turn navigation via Google Maps geo intents.
- `search_web`: Launches web browser searches.
- `open_settings`: Opens system settings or specific sub-settings (Wi-Fi, Bluetooth, Sound, Display).
- `save_memory`: Saves long-term personal facts.
- `get_memory`: Retrieves stored personal memories.
- `send_whatsapp`: Opens WhatsApp with a prefilled message.

### 5. Personal Memory (Room + Supabase PostgreSQL)
- Offline-first architecture using Room database.
- Cloud synchronization with Supabase PostgreSQL using Row Level Security (RLS).
- Tables: `profiles`, `conversations`, `messages`, `memories`, `reminders`, `custom_commands`, `assistant_settings`.
- Long-term memory categorization: `preference`, `fact`, `contact`, `routine`, `health`.

---

## 📱 The 12 Application Screens

1. **Splash Screen**: Animated glowing DOKI emblem and initialization checks.
2. **Onboarding Screen**: 4-step wizard introducing DOKI, privacy, multilingual selection, and credential setup.
3. **Permission Setup Screen**: Transparent permission manager for Microphone, Notifications, Phone, SMS, and Flashlight.
4. **Home Screen**: Main cockpit with DOKI greeting, dynamic time, interactive AI Orb, wake-word status pill, quick prompt suggestions, and text query input.
5. **Voice Assistant Screen**: Full-screen immersive voice overlay with real-time waveform, live transcription, tool execution cards, and interrupt controls.
6. **AI Chat Screen**: Multi-turn text conversation thread with message bubbles, tool execution tags, and streaming animations.
7. **Conversation History Screen**: Searchable history of past voice and text sessions.
8. **Memory Management Screen**: Search, categorize, add, and delete personal memories.
9. **Commands Screen**: Define and trigger custom voice routines and macros.
10. **Reminders Screen**: Manage alarms and time-based reminders created by DOKI.
11. **Settings Screen**: Full configuration panel for Gemini AI, Picovoice Porcupine, Voice pitch/rate, and Supabase sync.
12. **About DOKI Screen**: System specifications, architecture overview, and privacy pledge.

---

## 🏗 Project Architecture

Following **Clean Architecture** and **MVVM**:

```
com.doki.ai
├── DokiApplication.kt                 # Application class & DI root
├── MainActivity.kt                    # Single Activity hosting Jetpack Compose NavHost
├── core/
│   ├── ai/
│   │   ├── GeminiAiService.kt         # Gemini API client, system prompt builder, tool router
│   │   └── tools/
│   │       ├── AndroidToolRegistry.kt # Native Android device tool implementations
│   │       ├── ToolDefinition.kt      # Parameter schemas for tools
│   │       └── ToolExecutionResult.kt # Strongly-typed execution results
│   ├── common/                        # Result, UiState, and shared utilities
│   ├── designsystem/
│   │   ├── components/                # DokiOrb, WaveformVisualizer, GlowingMicButton, GlassCard
│   │   └── theme/                     # Color, Type, Theme
│   ├── permissions/                   # PermissionManager
│   ├── security/                      # SecurePreferences (DataStore)
│   └── voice/
│       ├── DokiSpeechRecognizer.kt    # Android SpeechRecognizer wrapper with RMS dB stream
│       ├── DokiTextToSpeech.kt        # Android TextToSpeech with pitch/rate/language
│       ├── PorcupineWakeWordManager.kt# Picovoice Porcupine SDK integration
│       └── VoiceState.kt              # IDLE, LISTENING, THINKING, SPEAKING, ERROR
├── data/
│   ├── local/
│   │   ├── DokiDatabase.kt            # Room database instance
│   │   ├── dao/                       # MemoryDao, ConversationDao, MessageDao, ReminderDao, CustomCommandDao
│   │   └── entity/                    # Room entities
│   ├── remote/
│   │   └── SupabaseClient.kt          # Ktor client for Supabase REST API
│   └── repository/                    # Repository implementations
├── domain/
│   ├── model/                         # Domain models (Memory, Conversation, Message, Language, etc.)
│   └── repository/                    # Repository interfaces
├── feature/                           # All 12 Compose screens and ViewModels
├── service/
│   ├── WakeWordService.kt             # Android Foreground Service for wake-word
│   ├── AssistantNotificationService.kt# Persistent notification with actions
│   ├── BootCompletedReceiver.kt       # Restarts wake word service on reboot
│   ├── DokiVoiceInteractionService.kt # VoiceInteractionService for default assistant
│   └── DokiVoiceInteractionSessionService.kt
└── di/
    └── AppContainer.kt                # Manual Dependency Injection container
```

---

## ⚙️ Setup & Configuration

### 1. Picovoice Porcupine Setup (Wake Word)
1. Sign up for a free account at [Picovoice Console](https://console.picovoice.ai/).
2. Copy your **AccessKey**.
3. Under **Porcupine**, train a custom keyword model for **"Hey Doki"** targeting Android.
4. Download the resulting `hey_doki_android.ppn` file and place it into:
   ```
   app/src/main/assets/hey_doki_android.ppn
   ```
5. Enter your Picovoice AccessKey in the DOKI app's **Onboarding** or **Settings** screen.
   *(Note: If you do not have an AccessKey yet, DOKI automatically switches to Push-to-Talk mode without crashing).*

### 2. Google Gemini Setup
1. Get a Gemini API Key from [Google AI Studio](https://aistudio.google.com/).
2. Enter your API key in DOKI **Settings** or **Onboarding**.
3. Select your desired model (default: `gemini-1.5-flash`).

### 3. Supabase Cloud Database Setup
1. Create a project on [Supabase](https://supabase.com).
2. Open the **SQL Editor** in the Supabase Dashboard.
3. Paste and run the entire contents of `supabase_schema.sql` located at the root of this project.
4. Copy your **Project URL** and **Anon Key** from Project Settings > API.
5. In DOKI Settings, paste the URL and Anon Key to enable cross-device cloud sync.

---

## 🔨 Build Instructions

### Prerequisites
- Android Studio Iguana (2023.2.1) or Ladybug (2024.2.1+)
- JDK 17
- Android SDK 35 (API Level 35)

### Build Debug APK via Command Line
```powershell
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit & ViewModel Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

### Build Release APK
1. Configure your keystore in `app/build.gradle.kts` under `signingConfigs`.
2. Run:
```powershell
.\gradlew.bat assembleRelease
```
The optimized release APK will be located at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## 🛡 Android Platform Permissions Guide

| Permission | Purpose | Required / Optional |
|---|---|---|
| `RECORD_AUDIO` | Speech-to-text and wake word | **Required** for voice assistant |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14+ background listening service | **Required** for wake-word |
| `POST_NOTIFICATIONS` | Android 13+ status notification | Recommended |
| `CALL_PHONE` | Initiating voice-requested phone calls | Optional (prompted upon use) |
| `SEND_SMS` | Sending voice-composed text messages | Optional (prompted upon use) |
| `CAMERA` | Flashlight toggle via CameraManager | Optional |
| `SET_ALARM` | Setting alarms in clock app | Optional |

---

## 📄 License
Production Architecture for DOKI AI Assistant. Built with modern Android best practices.

