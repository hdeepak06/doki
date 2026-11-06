package com.doki.ai.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiOrb
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.voice.VoiceState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isOnboardingCompleted: Boolean,
    onNavigateNext: (Boolean) -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        delay(1200)
        onNavigateNext(isOnboardingCompleted)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            DokiOrb(
                state = VoiceState.THINKING,
                size = 180.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "DOKI",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                color = DokiCyan
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PERSONAL AI ASSISTANT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = DokiTextSecondary
            )
        }
    }
}

