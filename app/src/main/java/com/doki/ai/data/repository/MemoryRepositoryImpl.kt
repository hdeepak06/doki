package com.doki.ai.data.repository

import com.doki.ai.data.local.dao.MemoryDao
import com.doki.ai.data.local.entity.MemoryEntity
import com.doki.ai.data.remote.SupabaseClient
import com.doki.ai.domain.model.Memory
import com.doki.ai.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao,
    private val supabaseClient: SupabaseClient
) : MemoryRepository {

    override fun getAllMemories(): Flow<List<Memory>> {
        return memoryDao.getAllMemories().map { list -> list.map { it.toDomain() } }
    }

    override fun getApprovedMemories(): Flow<List<Memory>> {
        return memoryDao.getApprovedMemories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun searchMemories(query: String): List<Memory> {
        return memoryDao.searchMemories(query).map { it.toDomain() }
    }

    override suspend fun getMemoryByKey(key: String): Memory? {
        return memoryDao.getMemoryByKey(key)?.toDomain()
    }

    override suspend fun saveMemory(memory: Memory) {
        val entity = MemoryEntity.fromDomain(memory)
        memoryDao.insertMemory(entity)

        // Sync with Supabase if online and configured
        if (supabaseClient.isConfigured) {
            runCatching {
                val jsonPayload = buildJsonObject {
                    put("id", memory.id)
                    put("category", memory.category)
                    put("key", memory.key)
                    put("value", memory.value)
                    put("confidence", memory.confidence)
                    put("is_approved", memory.isApproved)
                }.toString()
                supabaseClient.post("memories", jsonPayload)
            }
        }
    }

    override suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemoryById(id)
        if (supabaseClient.isConfigured) {
            runCatching {
                supabaseClient.delete("memories", "id=eq.$id")
            }
        }
    }

    override suspend fun clearAll() {
        memoryDao.clearAll()
    }

    override suspend fun syncWithCloud(): Result<Unit> {
        if (!supabaseClient.isConfigured) return Result.success(Unit)
        return runCatching {
            val response = supabaseClient.get<String>("memories", "select=*").getOrThrow()
            // Parse and sync to Room
        }
    }
}

