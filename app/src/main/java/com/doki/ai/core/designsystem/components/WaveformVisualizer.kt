package com.doki.ai.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiViolet
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    audioLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    height: Dp = 48.dp,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveformPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalSpacing = canvasWidth * 0.25f
        val barWidth = (canvasWidth - totalSpacing) / barCount
        val barGap = totalSpacing / (barCount - 1)

        val gradient = Brush.verticalGradient(
            colors = listOf(
                DokiCyan,
                DokiViolet
            )
        )

        for (i in 0 until barCount) {
            val progress = i.toFloat() / barCount
            val sineWave = (sin((phase + progress * 8f).toDouble()).toFloat() + 1f) / 2f
            val baseFactor = if (isActive) (0.2f + 0.8f * audioLevel) else 0.1f
            val barHeightFactor = (sineWave * baseFactor).coerceIn(0.08f, 1.0f)
            val currentBarHeight = canvasHeight * barHeightFactor

            val x = i * (barWidth + barGap)
            val y = (canvasHeight - currentBarHeight) / 2f

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

