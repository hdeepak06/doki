package com.doki.ai.data.repository

import com.doki.ai.data.local.dao.ReminderDao
import com.doki.ai.data.local.entity.ReminderEntity
import com.doki.ai.domain.model.Reminder
import com.doki.ai.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { list -> list.map { it.toDomain() } }
    }

    override fun getPendingReminders(): Flow<List<Reminder>> {
        return reminderDao.getPendingReminders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReminderById(id: String): Reminder? {
        return reminderDao.getReminderById(id)?.toDomain()
    }

    override suspend fun saveReminder(reminder: Reminder) {
        reminderDao.insertReminder(ReminderEntity.fromDomain(reminder))
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        reminderDao.setCompleted(id, completed)
    }

    override suspend fun deleteReminder(id: String) {
        reminderDao.deleteReminderById(id)
    }
}

