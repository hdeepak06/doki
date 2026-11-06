package com.doki.ai.feature.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.ai.tools.ToolExecutionResult
import com.doki.ai.core.designsystem.components.DokiOrb
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.components.GlowingMicButton
import com.doki.ai.core.designsystem.components.WaveformVisualizer
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiSuccessGreen
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.voice.VoiceState

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    onClose: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val assistantReply by viewModel.assistantReply.collectAsState()
    val activeTool by viewModel.activeToolResult.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()

    val scrollState = rememberScrollState()

    // Start listening on screen open
    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening..."
                        VoiceState.THINKING -> "Thinking..."
                        VoiceState.SPEAKING -> "DOKI is speaking"
                        VoiceState.ERROR -> "Error"
                        VoiceState.IDLE -> "Ready"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokiCyan
                )

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DokiTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Orb
            DokiOrb(
                state = voiceState,
                audioLevel = audioLevel,
                size = 240.dp,
                onClick = {
                    if (voiceState == VoiceState.SPEAKING) {
                        viewModel.stopSpeakingOrListening()
                    } else if (voiceState == VoiceState.IDLE || voiceState == VoiceState.ERROR) {
                        viewModel.startListening()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Real-time Sound Waveform
            WaveformVisualizer(
                audioLevel = audioLevel,
                isActive = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Live Content Area (Transcription & Response)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // User Transcript
                AnimatedVisibility(
                    visible = transcription.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "You said:",
                                fontSize = 12.sp,
                                color = DokiTextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$transcription\"",
                                fontSize = 16.sp,
                                color = DokiTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Tool Execution Result Badge
                activeTool?.let { toolResult ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handyman,
                                contentDescription = "Tool",
                                tint = DokiCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                val title = when (toolResult) {
                                    is ToolExecutionResult.Success -> "Executed: ${toolResult.toolName}"
                                    is ToolExecutionResult.RequiresConfirmation -> "Confirmation: ${toolResult.toolName}"
                                    is ToolExecutionResult.Error -> "Failed: ${toolResult.toolName}"
                                }
                                val desc = when (toolResult) {
                                    is ToolExecutionResult.Success -> toolResult.output
                                    is ToolExecutionResult.RequiresConfirmation -> toolResult.confirmationPrompt
                                    is ToolExecutionResult.Error -> toolResult.explanation
                                }
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    color = DokiSuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    fontSize = 13.sp,
                                    color = DokiTextSecondary
                                )
                            }
                        }
                    }
                }

                // Assistant Reply Card
                AnimatedVisibility(
                    visible = assistantReply.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "DOKI:",
                                fontSize = 12.sp,
                                color = DokiViolet,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = assistantReply,
                                fontSize = 16.sp,
                                color = DokiTextPrimary,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Mic Button (Tap to listen or stop)
            GlowingMicButton(
                state = voiceState,
                onClick = {
                    if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                        viewModel.stopSpeakingOrListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                size = 72.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (voiceState == VoiceState.SPEAKING) "Tap to interrupt" else "Tap to speak",
                fontSize = 12.sp,
                color = DokiTextMuted
            )
        }
    }
}

