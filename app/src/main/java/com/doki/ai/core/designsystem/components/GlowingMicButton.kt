package com.doki.ai.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.voice.VoiceState

@Composable
fun GlowingMicButton(
    state: VoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "MicGlow")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicGlowAlpha"
    )

    val isListening = state == VoiceState.LISTENING
    val isSpeaking = state == VoiceState.SPEAKING

    Box(
        modifier = modifier.size(size + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glowing animated outer aura when active
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isListening) DokiCyan.copy(alpha = glowAlpha) else DokiViolet.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Inner Action Button
        Box(
            modifier = Modifier
                .size(size)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = DokiCyan)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isListening) {
                            listOf(DokiCyan, DokiViolet)
                        } else if (isSpeaking) {
                            listOf(DokiViolet, DokiCyan)
                        } else {
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        }
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(DokiCyan, DokiViolet)),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (state) {
                VoiceState.LISTENING -> Icons.Default.GraphicEq
                VoiceState.SPEAKING -> Icons.Default.Stop
                else -> Icons.Default.Mic
            }
            val tint = if (isListening || isSpeaking) DokiBackground else DokiCyan

            Icon(
                imageVector = icon,
                contentDescription = "Microphone",
                tint = tint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

