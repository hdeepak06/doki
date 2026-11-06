package com.doki.ai.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Backgrounds
val DokiBackground = Color(0xFF060913)
val DokiDeepNavy = Color(0xFF0A0F1D)
val DokiSurfaceDark = Color(0xFF111827)
val DokiCardBackground = Color(0xCC0D1527)

// Neon & Futuristic Accents
val DokiCyan = Color(0xFF00F5FF)
val DokiCyanDark = Color(0xFF00B4D8)
val DokiViolet = Color(0xFF9D4EDD)
val DokiVioletDark = Color(0xFF7B2CBF)
val DokiElectricBlue = Color(0xFF3A86FF)

// Status & Indicators
val DokiSuccessGreen = Color(0xFF06D6A0)
val DokiWarningAmber = Color(0xFFFFB703)
val DokiErrorRed = Color(0xFFEF476F)

// Text Colors
val DokiTextPrimary = Color(0xFFF8FAFC)
val DokiTextSecondary = Color(0xFF94A3B8)
val DokiTextMuted = Color(0xFF64748B)

// Gradients
val DokiOrbGradient = Brush.radialGradient(
    colors = listOf(
        DokiCyan,
        DokiViolet,
        DokiDeepNavy
    )
)

val DokiCyberGradient = Brush.horizontalGradient(
    colors = listOf(
        DokiCyan,
        DokiViolet
    )
)

val DokiGlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x2200F5FF),
        Color(0x0A9D4EDD)
    )
)

