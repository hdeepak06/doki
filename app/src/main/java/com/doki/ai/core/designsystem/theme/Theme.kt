package com.doki.ai.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DokiCyan,
    onPrimary = DokiBackground,
    primaryContainer = DokiVioletDark,
    onPrimaryContainer = DokiTextPrimary,
    secondary = DokiViolet,
    onSecondary = DokiBackground,
    secondaryContainer = DokiDeepNavy,
    onSecondaryContainer = DokiTextPrimary,
    tertiary = DokiElectricBlue,
    background = DokiBackground,
    onBackground = DokiTextPrimary,
    surface = DokiDeepNavy,
    onSurface = DokiTextPrimary,
    surfaceVariant = DokiSurfaceDark,
    onSurfaceVariant = DokiTextSecondary,
    error = DokiErrorRed,
    onError = DokiBackground
)

@Composable
fun DokiAITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DokiBackground.toArgb()
            window.navigationBarColor = DokiBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DokiTypography,
        content = content
    )
}

