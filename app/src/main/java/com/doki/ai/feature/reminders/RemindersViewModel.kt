package com.doki.ai.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doki.ai.domain.model.Reminder
import com.doki.ai.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(title: String, minutesFromNow: Int, notes: String?) {
        if (title.isBlank()) return
        val dueTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
        viewModelScope.launch {
            reminderRepository.saveReminder(
                Reminder(
                    title = title.trim(),
                    notes = notes?.trim(),
                    dueTime = dueTime
                )
            )
        }
    }

    fun toggleCompleted(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.setCompleted(reminder.id, !reminder.isCompleted)
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(id)
        }
    }
}

