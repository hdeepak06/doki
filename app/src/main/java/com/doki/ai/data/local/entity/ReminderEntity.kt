package com.doki.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.doki.ai.domain.model.Reminder

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val userId: String?,
    val title: String,
    val notes: String?,
    val dueTime: Long,
    val isCompleted: Boolean,
    val isRecurring: Boolean,
    val recurrencePattern: String?,
    val alarmIntentUri: String?,
    val createdAt: Long
) {
    fun toDomain(): Reminder = Reminder(
        id = id,
        userId = userId,
        title = title,
        notes = notes,
        dueTime = dueTime,
        isCompleted = isCompleted,
        isRecurring = isRecurring,
        recurrencePattern = recurrencePattern,
        alarmIntentUri = alarmIntentUri,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(r: Reminder): ReminderEntity = ReminderEntity(
            id = r.id,
            userId = r.userId,
            title = r.title,
            notes = r.notes,
            dueTime = r.dueTime,
            isCompleted = r.isCompleted,
            isRecurring = r.isRecurring,
            recurrencePattern = r.recurrencePattern,
            alarmIntentUri = r.alarmIntentUri,
            createdAt = r.createdAt
        )
    }
}

