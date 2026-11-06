package com.doki.ai.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiOrb
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiDeepNavy
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.core.voice.VoiceState
import com.doki.ai.domain.model.Language
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val picovoiceKey by viewModel.picovoiceAccessKey.collectAsState()
    val supabaseUrl by viewModel.supabaseUrl.collectAsState()
    val supabaseKey by viewModel.supabaseAnonKey.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Stepper Dots
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) DokiCyan else DokiTextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> LanguageSelectionPage(
                    selectedLanguage = selectedLanguage,
                    onSelect = { viewModel.selectLanguage(it) }
                )
                2 -> PrivacyAndWakeWordPage()
                3 -> ConfigurationPage(
                    geminiKey = geminiKey,
                    picovoiceKey = picovoiceKey,
                    supabaseUrl = supabaseUrl,
                    supabaseKey = supabaseKey,
                    onGeminiKeyChange = { viewModel.updateGeminiApiKey(it) },
                    onPicovoiceKeyChange = { viewModel.updatePicovoiceAccessKey(it) },
                    onSupabaseUrlChange = { viewModel.updateSupabaseUrl(it) },
                    onSupabaseKeyChange = { viewModel.updateSupabaseAnonKey(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Footer Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Text("Back", color = DokiTextSecondary)
                }
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        viewModel.completeOnboarding(onFinish)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DokiCyan),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == 3) "Get Started" else "Next",
                    color = DokiBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DokiOrb(state = VoiceState.IDLE, size = 180.dp)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Meet DOKI",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = DokiTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your Alexa-like native Android AI companion powered by Gemini intelligence, multilingual voice understanding, and device control.",
            style = MaterialTheme.typography.bodyLarge,
            color = DokiTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun LanguageSelectionPage(
    selectedLanguage: Language,
    onSelect: (Language) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = null,
            tint = DokiCyan,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Choose Your Language",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DokiTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DOKI fluently understands and responds in English, Kannada (ಕನ್ನಡ), and Hindi (हिन्दी).",
            color = DokiTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Language.entries.forEach { lang ->
            val isSelected = lang == selectedLanguage
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onSelect(lang) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = lang.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DokiTextPrimary
                        )
                        Text(
                            text = lang.nativeName,
                            fontSize = 14.sp,
                            color = DokiCyan
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = DokiCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyAndWakeWordPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = DokiViolet,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Privacy & \"Hey DOKI\"",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DokiTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "🔒 Local Wake-Word Detection",
                    fontWeight = FontWeight.Bold,
                    color = DokiCyan
                )
                Text(
                    text = "Picovoice Porcupine runs 100% on-device. Your voice is never recorded or streamed anywhere until the wake phrase \"Hey DOKI\" is recognized.",
                    color = DokiTextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🛡 Least Permissions Necessary",
                    fontWeight = FontWeight.Bold,
                    color = DokiCyan
                )
                Text(
                    text = "DOKI does not request every Android permission upfront. Sensitive actions like placing calls or sending messages always ask for your explicit confirmation.",
                    color = DokiTextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ConfigurationPage(
    geminiKey: String,
    picovoiceKey: String,
    supabaseUrl: String,
    supabaseKey: String,
    onGeminiKeyChange: (String) -> Unit,
    onPicovoiceKeyChange: (String) -> Unit,
    onSupabaseUrlChange: (String) -> Unit,
    onSupabaseKeyChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            tint = DokiCyan,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Setup Credentials",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DokiTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter your keys now or configure them anytime in Settings.",
            color = DokiTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = onGeminiKeyChange,
            label = { Text("Google Gemini API Key") },
            placeholder = { Text("AIzaSy...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DokiCyan,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = DokiTextPrimary,
                unfocusedTextColor = DokiTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = picovoiceKey,
            onValueChange = onPicovoiceKeyChange,
            label = { Text("Picovoice Porcupine AccessKey") },
            placeholder = { Text("Enter AccessKey for wake word...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DokiCyan,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = DokiTextPrimary,
                unfocusedTextColor = DokiTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = supabaseUrl,
            onValueChange = onSupabaseUrlChange,
            label = { Text("Supabase Project URL (Optional)") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DokiViolet,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = DokiTextPrimary,
                unfocusedTextColor = DokiTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = supabaseKey,
            onValueChange = onSupabaseKeyChange,
            label = { Text("Supabase Anon Key (Optional)") },
            placeholder = { Text("ey...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DokiViolet,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = DokiTextPrimary,
                unfocusedTextColor = DokiTextPrimary
            )
        )
    }
}

