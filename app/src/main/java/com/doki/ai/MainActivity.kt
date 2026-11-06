package com.doki.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.doki.ai.core.designsystem.theme.DokiAITheme
import com.doki.ai.core.designsystem.theme.DokiBackground
import com.doki.ai.feature.navigation.DokiNavHost

class MainActivity : ComponentActivity() {

    private var startVoiceDirectly by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DokiApplication
        startVoiceDirectly = intent?.getBooleanExtra("EXTRA_START_VOICE", false) == true

        setContent {
            DokiAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DokiBackground
                ) {
                    val navController = rememberNavController()
                    DokiNavHost(
                        navController = navController,
                        container = app.container,
                        startVoiceDirectly = startVoiceDirectly
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_START_VOICE", false)) {
            startVoiceDirectly = true
        }
    }
}

