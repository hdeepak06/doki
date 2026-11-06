package com.doki.ai.core.ai.tools

import android.content.Context
import com.doki.ai.domain.model.Memory
import com.doki.ai.domain.repository.MemoryRepository
import com.doki.ai.domain.repository.ReminderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AndroidToolRegistryTest {

    private lateinit var mockContext: Context
    private lateinit var mockMemoryRepo: MemoryRepository
    private lateinit var mockReminderRepo: ReminderRepository
    private lateinit var toolRegistry: AndroidToolRegistry

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockMemoryRepo = mockk(relaxed = true)
        mockReminderRepo = mockk(relaxed = true)
        toolRegistry = AndroidToolRegistry(mockContext, mockMemoryRepo, mockReminderRepo)
    }

    @Test
    fun testToolRegistryContainsAllRequiredTools() {
        val registeredNames = toolRegistry.toolDefinitions.map { it.name }.toSet()
        val expected = setOf(
            "open_app",
            "make_call",
            "send_sms",
            "create_alarm",
            "create_reminder",
            "toggle_flashlight",
            "get_battery",
            "get_device_info",
            "open_navigation",
            "search_web",
            "open_settings",
            "save_memory",
            "get_memory",
            "send_whatsapp"
        )
        assertTrue(registeredNames.containsAll(expected))
    }

    @Test
    fun testSensitiveActionRequiresConfirmation() = runBlocking {
        // Calling make_call without confirmed=true should return RequiresConfirmation
        val result = toolRegistry.executeTool(
            name = "make_call",
            arguments = mapOf("phone_number" to "+1234567890"),
            confirmed = false
        )
        assertTrue(result is ToolExecutionResult.RequiresConfirmation)
        val confirmation = result as ToolExecutionResult.RequiresConfirmation
        assertEquals("make_call", confirmation.toolName)
        assertTrue(confirmation.confirmationPrompt.contains("+1234567890"))
    }

    @Test
    fun testSensitiveSmsRequiresConfirmation() = runBlocking {
        val result = toolRegistry.executeTool(
            name = "send_sms",
            arguments = mapOf("phone_number" to "9876543210", "message" to "Hello test"),
            confirmed = false
        )
        assertTrue(result is ToolExecutionResult.RequiresConfirmation)
    }

    @Test
    fun testInvalidToolReturnsError() = runBlocking {
        val result = toolRegistry.executeTool(
            name = "unknown_tool_xyz",
            arguments = emptyMap()
        )
        assertTrue(result is ToolExecutionResult.Error)
    }

    @Test
    fun testSaveMemoryTool() = runBlocking {
        val result = toolRegistry.executeTool(
            name = "save_memory",
            arguments = mapOf(
                "key" to "coffee_preference",
                "value" to "Espresso with almond milk",
                "category" to "preference"
            )
        )
        assertTrue(result is ToolExecutionResult.Success)
        coVerify { mockMemoryRepo.saveMemory(any()) }
    }

    @Test
    fun testGetMemoryTool() = runBlocking {
        coEvery { mockMemoryRepo.searchMemories("coffee") } returns listOf(
            Memory(key = "coffee", value = "Espresso with almond milk")
        )

        val result = toolRegistry.executeTool(
            name = "get_memory",
            arguments = mapOf("query" to "coffee")
        )
        assertTrue(result is ToolExecutionResult.Success)
        val success = result as ToolExecutionResult.Success
        assertTrue(success.output.contains("Espresso with almond milk"))
    }
}

