package com.doki.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.doki.ai.domain.model.Memory

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val category: String,
    val key: String,
    val value: String,
    val confidence: Float,
    val isApproved: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Memory = Memory(
        id = id,
        userId = userId,
        category = category,
        key = key,
        value = value,
        confidence = confidence,
        isApproved = isApproved,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(memory: Memory): MemoryEntity = MemoryEntity(
            id = memory.id,
            userId = memory.userId,
            category = memory.category,
            key = memory.key,
            value = memory.value,
            confidence = memory.confidence,
            isApproved = memory.isApproved,
            createdAt = memory.createdAt,
            updatedAt = memory.updatedAt
        )
    }
}

