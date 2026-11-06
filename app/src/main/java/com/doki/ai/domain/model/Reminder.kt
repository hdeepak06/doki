package com.doki.ai.domain.model

import java.util.UUID

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String,
    val notes: String? = null,
    val dueTime: Long,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null,
    val alarmIntentUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

