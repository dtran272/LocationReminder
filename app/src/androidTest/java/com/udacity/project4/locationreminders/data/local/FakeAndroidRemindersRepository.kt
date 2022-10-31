package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.dto.Result.Success
import com.udacity.project4.locationreminders.data.local.IRemindersRepository
import kotlinx.coroutines.runBlocking

class FakeAndroidRemindersRepository : IRemindersRepository {

    var remindersServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        remindersServiceData.let {
            return Success(it.values.toList())
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        remindersServiceData[id]?.let {
            return Success(it)
        }

        return Error("Could not find reminder.")
    }

    override suspend fun deleteAllReminders() {
        remindersServiceData.clear()
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersServiceData[reminder.id] = reminder
        }
    }
}