package com.doki.ai.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiTopBar
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.model.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var porcupineKey by remember(settings.porcupineAccessKey) { mutableStateOf(settings.porcupineAccessKey) }
    var supabaseUrl by remember(settings.supabaseUrl) { mutableStateOf(settings.supabaseUrl) }
    var supabaseKey by remember(settings.supabaseAnonKey) { mutableStateOf(settings.supabaseAnonKey) }

    var modelExpanded by remember { mutableStateOf(false) }
    val availableModels = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
    ) {
        DokiTopBar(
            title = "Settings",
            onBackClick = onBack,
            actions = {
                IconButton(onClick = onOpenAbout) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = DokiCyan
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AI Engine Configuration
            SectionTitle("Gemini AI Engine", Icons.Default.Psychology)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = {
                            geminiKey = it
                            viewModel.updateSettings(settings.copy(geminiApiKey = it))
                        },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DokiCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = DokiTextPrimary,
                            unfocusedTextColor = DokiTextPrimary
                        )
                    )

                    // Model Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded,
                        onExpandedChange = { modelExpanded = !modelExpanded }
                    ) {
                        OutlinedTextField(
                            value = settings.modelIdentifier,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model Identifier") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DokiCyan,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = DokiTextPrimary,
                                unfocusedTextColor = DokiTextPrimary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(modelIdentifier = model))
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Wake-Word Engine (Picovoice Porcupine)
            SectionTitle("Wake-Word Detection", Icons.Default.Mic)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wake Word \"Hey DOKI\"", fontWeight = FontWeight.Bold, color = DokiTextPrimary)
                            Text("Continuous background listening", fontSize = 12.sp, color = DokiTextSecondary)
                        }
                        Switch(
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(wakeWordEnabled = it), context)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DokiCyan)
                        )
                    }

                    OutlinedTextField(
                        value = porcupineKey,
                        onValueChange = {
                            porcupineKey = it
                            viewModel.updateSettings(settings.copy(porcupineAccessKey = it), context)
                        },
                        label = { Text("Picovoice Porcupine AccessKey") },
                        placeholder = { Text("Enter AccessKey from console.picovoice.ai") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DokiCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = DokiTextPrimary,
                            unfocusedTextColor = DokiTextPrimary
                        )
                    )

                    Text(
                        text = "Sensitivity: ${(settings.wakeWordSensitivity * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = DokiTextSecondary
                    )
                    Slider(
                        value = settings.wakeWordSensitivity,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(wakeWordSensitivity = it))
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = DokiCyan, activeTrackColor = DokiCyan)
                    )

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = DokiCyan)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Disable Battery Optimization", color = DokiCyan, fontSize = 13.sp)
                    }
                }
            }

            // 3. Multilingual & Voice Settings
            SectionTitle("Multilingual Voice & TTS", Icons.Default.Language)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Preferred Language", fontWeight = FontWeight.Bold, color = DokiTextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Language.entries.forEach { lang ->
                            val isSelected = lang == settings.preferredLanguage
                            Button(
                                onClick = { viewModel.updateSettings(settings.copy(preferredLanguage = lang)) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) DokiCyan else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = lang.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DokiBackground else DokiTextSecondary
                                )
                            }
                        }
                    }

                    Text("Speech Rate: ${String.format("%.1fx", settings.voiceSpeed)}", fontSize = 13.sp, color = DokiTextSecondary)
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = { viewModel.updateSettings(settings.copy(voiceSpeed = it)) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = DokiViolet, activeTrackColor = DokiViolet)
                    )

                    Text("Voice Pitch: ${String.format("%.1fx", settings.voicePitch)}", fontSize = 13.sp, color = DokiTextSecondary)
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { viewModel.updateSettings(settings.copy(voicePitch = it)) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = DokiViolet, activeTrackColor = DokiViolet)
                    )

                    OutlinedButton(
                        onClick = { viewModel.testVoice(settings) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = DokiViolet)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Preview DOKI Voice", color = DokiViolet)
                    }
                }
            }

            // 4. Cloud Supabase Settings
            SectionTitle("Supabase Cloud Sync", Icons.Default.Key)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = {
                            supabaseUrl = it
                            viewModel.updateSettings(settings.copy(supabaseUrl = it))
                        },
                        label = { Text("Supabase URL") },
                        placeholder = { Text("https://your-project.supabase.co") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DokiViolet,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = DokiTextPrimary,
                            unfocusedTextColor = DokiTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = {
                            supabaseKey = it
                            viewModel.updateSettings(settings.copy(supabaseAnonKey = it))
                        },
                        label = { Text("Supabase Anon Key") },
                        placeholder = { Text("eyJ...") },
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

            // Permissions Dashboard Shortcut
            OutlinedButton(
                onClick = onOpenPermissions,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Manage Android Permissions", color = DokiCyan)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = DokiCyan, modifier = Modifier.size(20.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DokiTextPrimary)
    }
}

