package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

interface IRemindersRepository {
    suspend fun getReminders(): Result<List<ReminderDTO>>

    suspend fun saveReminder(reminder: ReminderDTO)

    suspend fun getReminder(id: String): Result<ReminderDTO>

    suspend fun deleteAllReminders()
}