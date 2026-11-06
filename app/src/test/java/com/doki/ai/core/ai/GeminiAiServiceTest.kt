package com.doki.ai.core.ai

import com.doki.ai.core.ai.tools.AndroidToolRegistry
import com.doki.ai.domain.model.Language
import com.doki.ai.domain.model.Memory
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiAiServiceTest {

    private lateinit var toolRegistry: AndroidToolRegistry
    private lateinit var geminiService: GeminiAiService

    @Before
    fun setup() {
        toolRegistry = mockk(relaxed = true)
        geminiService = GeminiAiService(toolRegistry)
    }

    @Test
    fun testMissingApiKeyReturnsFailure() = runBlocking {
        val result = geminiService.generateResponse(
            apiKey = "",
            modelName = "gemini-1.5-flash",
            history = emptyList(),
            userPrompt = "Hello DOKI",
            userMemories = emptyList(),
            preferredLanguage = Language.ENGLISH
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key is not configured") == true)
    }
}

