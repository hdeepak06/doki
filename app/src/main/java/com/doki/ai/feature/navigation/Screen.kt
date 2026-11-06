package com.doki.ai.feature.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Permissions : Screen("permissions")
    data object Home : Screen("home")
    data object VoiceAssistant : Screen("voice_assistant")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String = "new") = "chat/$conversationId"
    }
    data object History : Screen("history")
    data object Memory : Screen("memory")
    data object Commands : Screen("commands")
    data object Reminders : Screen("reminders")
    data object Settings : Screen("settings")
    data object About : Screen("about")
}

