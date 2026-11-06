package com.doki.ai.feature.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.doki.ai.di.AppContainer
import com.doki.ai.feature.about.AboutDokiScreen
import com.doki.ai.feature.assistant.VoiceAssistantScreen
import com.doki.ai.feature.assistant.VoiceAssistantViewModel
import com.doki.ai.feature.chat.AiChatScreen
import com.doki.ai.feature.chat.AiChatViewModel
import com.doki.ai.feature.commands.CommandsScreen
import com.doki.ai.feature.commands.CommandsViewModel
import com.doki.ai.feature.history.ConversationHistoryScreen
import com.doki.ai.feature.history.ConversationHistoryViewModel
import com.doki.ai.feature.home.HomeScreen
import com.doki.ai.feature.home.HomeViewModel
import com.doki.ai.feature.memory.MemoryManagementScreen
import com.doki.ai.feature.memory.MemoryViewModel
import com.doki.ai.feature.onboarding.OnboardingScreen
import com.doki.ai.feature.onboarding.OnboardingViewModel
import com.doki.ai.feature.permissions.PermissionSetupScreen
import com.doki.ai.feature.permissions.PermissionViewModel
import com.doki.ai.feature.reminders.RemindersScreen
import com.doki.ai.feature.reminders.RemindersViewModel
import com.doki.ai.feature.settings.SettingsScreen
import com.doki.ai.feature.settings.SettingsViewModel
import com.doki.ai.feature.splash.SplashScreen

@Composable
inline fun <reified VM : ViewModel> dokiViewModel(crossinline creator: () -> VM): VM {
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return creator() as T
            }
        }
    }
    return viewModel(factory = factory)
}

@Composable
fun DokiNavHost(
    navController: NavHostController,
    container: AppContainer,
    startVoiceDirectly: Boolean = false
) {
    val isOnboardingCompleted by container.settingsRepository.isOnboardingCompleted.collectAsState(initial = false)

    val startDestination = if (startVoiceDirectly) Screen.VoiceAssistant.route else Screen.Splash.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                isOnboardingCompleted = isOnboardingCompleted,
                onNavigateNext = { completed ->
                    if (completed) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 2. Onboarding Screen
        composable(Screen.Onboarding.route) {
            val vm = dokiViewModel { OnboardingViewModel(container.settingsRepository) }
            OnboardingScreen(
                viewModel = vm,
                onFinish = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Permission Setup Screen
        composable(Screen.Permissions.route) {
            val vm = dokiViewModel { PermissionViewModel() }
            PermissionSetupScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onProceed = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Home Screen
        composable(Screen.Home.route) {
            val vm = dokiViewModel {
                HomeViewModel(
                    container.settingsRepository,
                    container.wakeWordManager
                )
            }
            HomeScreen(
                viewModel = vm,
                onOpenVoiceAssistant = { navController.navigate(Screen.VoiceAssistant.route) },
                onOpenChat = { prompt -> navController.navigate("chat/new?prompt=$prompt") },
                onOpenHistory = { navController.navigate(Screen.History.route) },
                onOpenMemory = { navController.navigate(Screen.Memory.route) },
                onOpenCommands = { navController.navigate(Screen.Commands.route) },
                onOpenReminders = { navController.navigate(Screen.Reminders.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 5. Voice Assistant Screen
        composable(Screen.VoiceAssistant.route) {
            val vm = dokiViewModel {
                VoiceAssistantViewModel(
                    container.speechRecognizer,
                    container.textToSpeech,
                    container.geminiAiService,
                    container.memoryRepository,
                    container.conversationRepository,
                    container.settingsRepository
                )
            }
            VoiceAssistantScreen(
                viewModel = vm,
                onClose = { navController.popBackStack() }
            )
        }

        // 6. AI Chat Screen
        composable(
            route = "chat/{conversationId}?prompt={prompt}",
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType; defaultValue = "new" },
                navArgument("prompt") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val convId = backStackEntry.arguments?.getString("conversationId") ?: "new"
            val prompt = backStackEntry.arguments?.getString("prompt")
            val vm = dokiViewModel {
                AiChatViewModel(
                    container.conversationRepository,
                    container.memoryRepository,
                    container.settingsRepository,
                    container.geminiAiService
                )
            }
            AiChatScreen(
                conversationId = convId,
                initialPrompt = prompt,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // 7. Conversation History Screen
        composable(Screen.History.route) {
            val vm = dokiViewModel { ConversationHistoryViewModel(container.conversationRepository) }
            ConversationHistoryScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenConversation = { id -> navController.navigate("chat/$id") }
            )
        }

        // 8. Memory Management Screen
        composable(Screen.Memory.route) {
            val vm = dokiViewModel { MemoryViewModel(container.memoryRepository) }
            MemoryManagementScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // 9. Commands Screen
        composable(Screen.Commands.route) {
            val vm = dokiViewModel {
                CommandsViewModel(
                    container.commandRepository,
                    container.toolRegistry
                )
            }
            CommandsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Reminders Screen
        composable(Screen.Reminders.route) {
            val vm = dokiViewModel { RemindersViewModel(container.reminderRepository) }
            RemindersScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // 11. Settings Screen
        composable(Screen.Settings.route) {
            val vm = dokiViewModel {
                SettingsViewModel(
                    container.settingsRepository,
                    container.textToSpeech
                )
            }
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Screen.About.route) },
                onOpenPermissions = { navController.navigate(Screen.Permissions.route) }
            )
        }

        // 12. About DOKI Screen
        composable(Screen.About.route) {
            AboutDokiScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
