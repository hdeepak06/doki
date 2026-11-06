package com.doki.ai.feature.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doki.ai.core.designsystem.components.DokiTopBar
import com.doki.ai.core.designsystem.components.GlassCard
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.core.designsystem.theme.DokiCyan
import com.doki.ai.core.designsystem.theme.DokiSuccessGreen
import com.doki.ai.core.designsystem.theme.DokiTextPrimary
import com.doki.ai.core.designsystem.theme.DokiTextSecondary
import com.doki.ai.core.designsystem.theme.DokiWarningAmber
import com.doki.ai.core.permissions.PermissionItem
import com.doki.ai.core.permissions.PermissionManager

@Composable
fun PermissionSetupScreen(
    viewModel: PermissionViewModel,
    onBack: () -> Unit,
    onProceed: () -> Unit
) {
    val context = LocalContext.current
    val permissions by viewModel.permissions.collectAsState()

    var activePermissionRequest by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
        viewModel.refreshPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DokiBackground)
    ) {
        DokiTopBar(
            title = "Permissions",
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android Security & Permissions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokiTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DOKI strictly honors Android platform restrictions. Enable microphone for voice recognition, or grant optional permissions for device actions.",
                    fontSize = 14.sp,
                    color = DokiTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(permissions, key = { it.permission }) { item ->
                val isGranted = PermissionManager.isPermissionGranted(context, item.permission)
                PermissionCard(
                    item = item,
                    isGranted = isGranted,
                    onRequest = {
                        activePermissionRequest = item.permission
                        permissionLauncher.launch(item.permission)
                    }
                )
            }
        }

        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DokiCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Continue",
                fontWeight = FontWeight.Bold,
                color = DokiBackground
            )
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DokiTextPrimary
                    )
                    if (item.isRequired) {
                        Text(
                            text = "Required",
                            fontSize = 11.sp,
                            color = DokiCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = DokiTextSecondary
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = DokiSuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Active", color = DokiSuccessGreen, fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Grant", color = DokiCyan, fontSize = 12.sp)
                }
            }
        }
    }
}

