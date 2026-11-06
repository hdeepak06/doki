package com.doki.ai.feature.commands

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiTopBar
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiSuccessGreen
import com.doki.ai.core.designsystem.theme.DokiTextMuted
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiViolet
import com.doki.ai.domain.model.CustomCommand

@Composable
fun CommandsScreen(
    viewModel: CommandsViewModel,
    onBack: () -> Unit
) {
    val commands by viewModel.commands.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var triggerPhrase by remember { mutableStateOf("") }
    var selectedTool by remember { mutableStateOf("toggle_flashlight") }
    var paramsJson by remember { mutableStateOf("{\"state\": true}") }

    Scaffold(
        topBar = {
            DokiTopBar(title = "Voice Commands", onBackClick = onBack)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DokiCyan,
                contentColor = DokiBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Command")
            }
        },
        containerColor = DokiBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Voice Macros & Routines",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DokiTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Define custom trigger phrases to run automated Android device actions.",
                fontSize = 13.sp,
                color = DokiTextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = DokiTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No custom commands set",
                            color = DokiTextSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' to create shortcuts like \"Bedtime\" or \"Night Mode\".",
                            color = DokiTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(commands, key = { it.id }) { cmd ->
                        CommandItemCard(
                            command = cmd,
                            onRun = { viewModel.executeCommand(cmd) },
                            onDelete = { viewModel.deleteCommand(cmd) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Custom Command") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = triggerPhrase,
                        onValueChange = { triggerPhrase = it },
                        label = { Text("When I say...") },
                        placeholder = { Text("e.g. Turn on night light") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedTool,
                        onValueChange = { selectedTool = it },
                        label = { Text("Tool to Execute") },
                        placeholder = { Text("e.g. toggle_flashlight, create_alarm") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = paramsJson,
                        onValueChange = { paramsJson = it },
                        label = { Text("Tool Parameters (JSON)") },
                        placeholder = { Text("{\"state\": true}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (triggerPhrase.isNotBlank() && selectedTool.isNotBlank()) {
                            viewModel.createCommand(triggerPhrase, selectedTool, paramsJson)
                            triggerPhrase = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DokiCyan)
                ) {
                    Text("Create", color = DokiBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DokiTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CommandItemCard(
    command: CustomCommand,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"${command.triggerPhrase}\"",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokiCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = command.actionPayload,
                    fontSize = 12.sp,
                    color = DokiTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRun) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = DokiSuccessGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DokiTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

