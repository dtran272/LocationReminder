package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.utils.wrapEspressoIdlingResource
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
        wrapEspressoIdlingResource {
            return remindersLocalDataSource.getReminders()
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        wrapEspressoIdlingResource {
            coroutineScope {
                remindersLocalDataSource.saveReminder(reminder)
            }
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        wrapEspressoIdlingResource {
            return remindersLocalDataSource.getReminder(id)
        }
    }

    override suspend fun deleteAllReminders() {
        wrapEspressoIdlingResource {
            withContext(ioDispatcher) {
                coroutineScope {
                    launch { remindersLocalDataSource.deleteAllReminders() }
                }
            }
        }
    }
}
