package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.*

/**
 * Concrete implementation of a data source as a db.
 *
 * The repository is implemented so that you can focus on only testing it.
 *
 */
class RemindersLocalRepository(
    private val remindersLocalDataSource: ReminderDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IRemindersRepository {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return remindersLocalDataSource.getReminders()
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        coroutineScope {
            remindersLocalDataSource.saveReminder(reminder)
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return remindersLocalDataSource.getReminder(id)
    }

    override suspend fun deleteAllReminders() {
        withContext(ioDispatcher) {
            coroutineScope {
                launch { remindersLocalDataSource.deleteAllReminders() }
            }
        }
    }
}
