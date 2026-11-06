package com.doki.ai.domain.repository

import com.doki.ai.domain.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemories(): Flow<List<Memory>>
    fun getApprovedMemories(): Flow<List<Memory>>
    suspend fun searchMemories(query: String): List<Memory>
    suspend fun getMemoryByKey(key: String): Memory?
    suspend fun saveMemory(memory: Memory)
    suspend fun deleteMemory(id: String)
    suspend fun clearAll()
    suspend fun syncWithCloud(): Result<Unit>
}

