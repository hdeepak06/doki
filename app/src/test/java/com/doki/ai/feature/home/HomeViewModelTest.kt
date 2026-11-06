package com.doki.ai.feature.home

import com.doki.ai.core.voice.PorcupineWakeWordManager
import com.doki.ai.domain.model.AssistantSettings
import com.doki.ai.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var wakeWordManager: PorcupineWakeWordManager
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)
        wakeWordManager = mockk(relaxed = true)

        every { settingsRepository.settingsFlow } returns flowOf(AssistantSettings())
        every { wakeWordManager.isListening } returns flowOf(false)
        every { wakeWordManager.statusMessage } returns flowOf("Standby")
        every { wakeWordManager.isPushToTalkFallback } returns flowOf(true)

        viewModel = HomeViewModel(settingsRepository, wakeWordManager)
    }

    @Test
    fun testGreetingIsNotEmpty() {
        val greeting = viewModel.getGreeting()
        assertNotNull(greeting)
        assertTrue(greeting.contains("DOKI"))
    }

    @Test
    fun testQueryTextChange() {
        viewModel.onTextQueryChange("Turn on flashlight")
        org.junit.Assert.assertEquals("Turn on flashlight", viewModel.textQuery.value)
    }
}

