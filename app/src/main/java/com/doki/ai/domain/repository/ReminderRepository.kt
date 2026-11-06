package com.doki.ai.domain.repository

import com.doki.ai.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getPendingReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun saveReminder(reminder: Reminder)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun deleteReminder(id: String)
}

