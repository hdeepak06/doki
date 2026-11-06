package com.doki.ai.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiElectricBlue
import com.doki.ai.core.designsystem.theme.DokiErrorRed
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.designsystem.theme.DokiVioletDark
import com.doki.ai.core.designsystem.theme.DokiWarningAmber
import com.doki.ai.core.voice.VoiceState
import kotlin.math.sin

@Composable
fun DokiOrb(
    state: VoiceState,
    audioLevel: Float = 0f, // 0.0 to 1.0 from microphone
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Slow breathing animation (Idle & Speaking)
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreath"
    )

    // Fast rotation (Thinking & Listening)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Counter rotation for dual swirling rings
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbCounterRotation"
    )

    // Pulse phase for ripples
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbPulse"
    )

    // Error jitter vibration
    val jitterOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbJitter"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(
                x = size.toPx() / 2f + if (state == VoiceState.ERROR) jitterOffset else 0f,
                y = size.toPx() / 2f
            )
            val baseRadius = (size.toPx() / 2f) * 0.70f

            when (state) {
                VoiceState.IDLE -> {
                    // Gentle breathing cyan/violet core
                    val radius = baseRadius * breathScale

                    // Outer soft glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DokiCyan.copy(alpha = 0.25f),
                                DokiViolet.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = center
                    )

                    // Inner glowing sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                DokiCyan.copy(alpha = 0.85f),
                                DokiViolet.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Subtle celestial ring
                    rotate(rotationAngle * 0.5f, pivot = center) {
                        drawArc(
                            color = DokiCyan.copy(alpha = 0.4f),
                            startAngle = 0f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 1.15f),
                            size = Size(radius * 2.3f, radius * 2.3f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                VoiceState.LISTENING -> {
                    // Audio-reactive concentric expanding rings
                    val dynamicRadius = baseRadius * (1f + (audioLevel * 0.45f))

                    // Expanding listening ripples
                    for (i in 0..2) {
                        val rippleFraction = (pulsePhase + i * 0.33f) % 1f
                        val rippleRadius = baseRadius * (1f + rippleFraction * 0.8f)
                        val rippleAlpha = (1f - rippleFraction) * 0.6f

                        drawCircle(
                            color = DokiCyan.copy(alpha = rippleAlpha),
                            radius = rippleRadius,
                            center = center,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Bright vibrant cyan core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                DokiCyan,
                                DokiCyanDark,
                                Color.Transparent
                            ),
                            center = center,
                            radius = dynamicRadius
                        ),
                        radius = dynamicRadius,
                        center = center
                    )
                }

                VoiceState.THINKING -> {
                    // Fast swirling orbiting dual rings with energetic cyan-violet shimmer
                    val radius = baseRadius * 0.95f

                    // Central pulsing nucleus
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                DokiViolet,
                                DokiCyan,
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 0.85f
                        ),
                        radius = radius * 0.85f,
                        center = center
                    )

                    // Outer Orbiting Cyan Ring
                    rotate(rotationAngle, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(DokiCyan, Color.Transparent, DokiCyan),
                                center = center
                            ),
                            startAngle = 0f,
                            sweepAngle = 260f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.25f),
                            size = Size(radius * 2.5f, radius * 2.5f),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Counter-rotating Violet Ring
                    rotate(counterRotation, pivot = center) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(DokiViolet, Color.Transparent, DokiViolet),
                                center = center
                            ),
                            startAngle = 90f,
                            sweepAngle = 220f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius * 1.05f, center.y - radius * 1.05f),
                            size = Size(radius * 2.1f, radius * 2.1f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                VoiceState.SPEAKING -> {
                    // Dancing speech frequency ripples with electric violet dominance
                    val speechScale = 1f + 0.15f * sin((pulsePhase * 6.28f).toDouble()).toFloat()
                    val radius = baseRadius * speechScale

                    // Outer harmonic aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DokiViolet.copy(alpha = 0.5f),
                                DokiElectricBlue.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )

                    // Core glowing sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                DokiViolet,
                                DokiVioletDark,
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Dynamic wave ring
                    rotate(rotationAngle * 0.3f, pivot = center) {
                        drawArc(
                            color = DokiCyan,
                            startAngle = 45f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 1.15f),
                            size = Size(radius * 2.3f, radius * 2.3f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                VoiceState.ERROR -> {
                    // Warning amber/red tint with unstable jitter
                    val radius = baseRadius

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DokiErrorRed.copy(alpha = 0.8f),
                                DokiWarningAmber.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.3f
                        ),
                        radius = radius * 1.3f,
                        center = center
                    )

                    drawCircle(
                        color = DokiErrorRed,
                        radius = radius * 0.7f,
                        center = center
                    )
                }
            }
        }
    }
}

