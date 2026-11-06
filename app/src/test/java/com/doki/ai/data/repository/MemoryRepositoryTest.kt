package com.doki.ai.data.repository

import com.doki.ai.data.local.dao.MemoryDao
import com.doki.ai.data.local.entity.MemoryEntity
import com.doki.ai.data.remote.SupabaseClient
import com.doki.ai.domain.model.Memory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MemoryRepositoryTest {

    private lateinit var memoryDao: MemoryDao
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var repository: MemoryRepositoryImpl

    @Before
    fun setup() {
        memoryDao = mockk(relaxed = true)
        supabaseClient = mockk(relaxed = true)
        repository = MemoryRepositoryImpl(memoryDao, supabaseClient)
    }

    @Test
    fun testSaveMemoryPersistsToDao() = runBlocking {
        val memory = Memory(key = "test_key", value = "test_value")
        repository.saveMemory(memory)

        coVerify { memoryDao.insertMemory(match { it.key == "test_key" && it.value == "test_value" }) }
    }

    @Test
    fun testGetMemoryListFlow() = runBlocking {
        val entity = MemoryEntity(
            id = "1",
            userId = null,
            category = "preference",
            key = "drink",
            value = "chai",
            confidence = 1f,
            isApproved = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        coEvery { memoryDao.getAllMemories() } returns flowOf(listOf(entity))

        val result = repository.getAllMemories().first()
        assertEquals(1, result.size)
        assertEquals("drink", result.first().key)
        assertEquals("chai", result.first().value)
    }
}

