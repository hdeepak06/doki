package com.doki.ai.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiOrb
import com.doki.ai.core.designsystem.components.GlowingMicButton
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiDeepNavy
import com.doki.ai.core.designsystem.theme.DokiSuccessGreen
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.designsystem.theme.DokiWarningAmber
import com.doki.ai.core.voice.VoiceState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenVoiceAssistant: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val voiceState by viewModel.voiceState.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val textQuery by viewModel.textQuery.collectAsState()
    val isWakeWordListening by viewModel.isWakeWordListening.collectAsState()
    val wakeWordStatus by viewModel.wakeWordStatus.collectAsState()
    val isPushToTalk by viewModel.isPushToTalk.collectAsState()

    val verticalScroll = rememberScrollState()
    val quickActionsScroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScroll)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar Shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(DokiCyan)
                    )
                    Text(
                        text = "DOKI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = DokiTextPrimary
                    )
                }

                // Shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = DokiCyan)
                    }
                    IconButton(onClick = onOpenReminders) {
                        Icon(Icons.Default.Alarm, contentDescription = "Reminders", tint = DokiCyan)
                    }
                    IconButton(onClick = onOpenMemory) {
                        Icon(Icons.Default.Psychology, contentDescription = "Memory", tint = DokiCyan)
                    }
                    IconButton(onClick = onOpenCommands) {
                        Icon(Icons.Default.Terminal, contentDescription = "Commands", tint = DokiCyan)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = DokiCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Greeting & Time Header
            Text(
                text = viewModel.getGreeting(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DokiTextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentDate,
                    fontSize = 14.sp,
                    color = DokiTextSecondary
                )
                Text(
                    text = currentTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokiCyan
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Wake-Word Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(
                        1.dp,
                        if (isWakeWordListening) DokiSuccessGreen.copy(alpha = 0.5f) else Color(0xFF334155),
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { viewModel.toggleWakeWordService(context) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isWakeWordListening) DokiSuccessGreen
                                else if (isPushToTalk) DokiWarningAmber
                                else DokiTextMuted
                            )
                    )
                    Text(
                        text = if (isWakeWordListening) "Wake phrase active: \"Hey DOKI\""
                        else if (isPushToTalk) "Push-to-Talk active (Tap to wake)"
                        else "Wake word standby (Tap to activate)",
                        fontSize = 12.sp,
                        color = DokiTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Interactive Centerpiece: DokiOrb
            DokiOrb(
                state = voiceState,
                size = 230.dp,
                onClick = onOpenVoiceAssistant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Tap orb or mic to speak with DOKI",
                fontSize = 13.sp,
                color = DokiTextMuted
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Prompt Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(quickActionsScroll),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickChip("🔋 Battery status") { onOpenChat("What's my battery percentage?") }
                QuickChip("🔦 Flashlight on") { onOpenChat("Turn on flashlight") }
                QuickChip("⏰ Alarm at 7 AM") { onOpenChat("Set alarm for 7:00 AM") }
                QuickChip("ಕನ್ನಡದಲ್ಲಿ ಮಾತಾಡು") { onOpenChat("ಹಲೋ ಡೋಕಿ, ಹೇಗಿದ್ದೀಯ?") }
                QuickChip("हिंदी में बात करो") { onOpenChat("नमस्ते डोकी, आज का मौसम कैसा है?") }
                QuickChip("💬 WhatsApp") { onOpenChat("Open WhatsApp and send message") }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Input Box for typed interaction
            OutlinedTextField(
                value = textQuery,
                onValueChange = { viewModel.onTextQueryChange(it) },
                placeholder = { Text("Ask DOKI anything...", color = DokiTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textQuery.isNotBlank()) {
                        val query = textQuery
                        viewModel.onTextQueryChange("")
                        onOpenChat(query)
                    }
                }),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (textQuery.isNotBlank()) {
                                val query = textQuery
                                viewModel.onTextQueryChange("")
                                onOpenChat(query)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (textQuery.isNotBlank()) DokiCyan else DokiTextMuted
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DokiCyan,
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = DokiDeepNavy,
                    unfocusedContainerColor = DokiDeepNavy,
                    focusedTextColor = DokiTextPrimary,
                    unfocusedTextColor = DokiTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(96.dp)) // Padding for bottom mic
        }

        // Docked Glowing Mic Button at bottom center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            GlowingMicButton(
                state = voiceState,
                onClick = onOpenVoiceAssistant,
                size = 76.dp
            )
        }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = DokiTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

