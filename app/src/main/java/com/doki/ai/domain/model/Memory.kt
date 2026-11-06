package com.doki.ai.domain.model

import java.util.UUID

data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val category: String = "general", // 'preference', 'fact', 'contact', 'routine', 'health'
    val key: String,
    val value: String,
    val confidence: Float = 1.0f,
    val isApproved: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

