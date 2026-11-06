package com.doki.ai.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiOrb
import com.doki.ai.core.designsystem.components.DokiTopBar
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.voice.VoiceState

@Composable
fun AboutDokiScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
    ) {
        DokiTopBar(title = "About DOKI", onBackClick = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DokiOrb(state = VoiceState.SPEAKING, size = 140.dp)

            Text(
                text = "DOKI AI ASSISTANT",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = DokiCyan
            )
            Text(
                text = "Version 1.0.0 (Production Architecture)",
                fontSize = 12.sp,
                color = DokiTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Architecture Specifications", fontWeight = FontWeight.Bold, color = DokiTextPrimary)
                    SpecRow("Platform", "Android (API 26 - 35)")
                    SpecRow("UI Toolkit", "Jetpack Compose (Material3)")
                    SpecRow("Architecture", "Clean Architecture + MVVM")
                    SpecRow("AI Engine", "Google Gemini (1.5 Flash / Pro)")
                    SpecRow("Wake Word", "Picovoice Porcupine (Hey DOKI)")
                    SpecRow("Speech Recognizer", "Android SpeechRecognizer")
                    SpecRow("Speech Synthesizer", "Android TextToSpeech")
                    SpecRow("Local Storage", "Room DB + DataStore")
                    SpecRow("Cloud Sync", "Supabase PostgreSQL")
                    SpecRow("Languages", "English, Kannada, Hindi")
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = DokiCyan, modifier = Modifier.size(18.dp))
                        Text("Privacy Guarantee", fontWeight = FontWeight.Bold, color = DokiTextPrimary)
                    }
                    Text(
                        text = "• Wake-word detection occurs strictly locally on your device hardware.\n" +
                                "• No continuous background audio is ever sent to any cloud server.\n" +
                                "• Sensitive actions like calling or texting always require your confirmation.\n" +
                                "• Personal memories can be inspected and deleted anytime.",
                        fontSize = 13.sp,
                        color = DokiTextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = DokiTextMuted)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DokiTextPrimary)
    }
}

