package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersLocalDataSourceTest {
    private val REMINDER1 = ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1")
    private val REMINDER2 = ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var database: RemindersDatabase

    private lateinit var localDataSource: RemindersLocalDataSource

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()

        localDataSource = RemindersLocalDataSource(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun saveReminder_successfullySavesReminder() = mainCoroutineRule.runBlockingTest {
        // given
        val expectedResult = Result.Success(REMINDER1)

        // when
        database.reminderDao().saveReminder(REMINDER1)

        // then
        val result = localDataSource.getReminder(REMINDER1.id) as Result.Success

        assertThat(result, `is`(expectedResult))
        assertThat(result.data, `is`(expectedResult.data))
    }

    @Test
    fun getReminder_reminderFound_returnsSuccessResult() = mainCoroutineRule.runBlockingTest {
        // given
        val expectedResult = Result.Success(REMINDER1)
        database.reminderDao().saveReminder(REMINDER1)

        // when
        val result = localDataSource.getReminder(REMINDER1.id) as Result.Success

        // then
        assertThat(result, `is`(expectedResult))
        assertThat(result.data, `is`(expectedResult.data))
    }

    @Test
    fun getReminder_reminderNotFound_returnsErrorResult() = mainCoroutineRule.runBlockingTest {
        // given
        val expectedResult = Result.Error("Reminder not found!")
        database.reminderDao().saveReminder(REMINDER1)

        // when
        val result = localDataSource.getReminder(REMINDER2.id) as Result.Error

        // then
        assertThat(result, `is`(expectedResult))
    }

    @Test
    fun getReminders_returnsAllReminders() = mainCoroutineRule.runBlockingTest {
        // given
        database.reminderDao().saveReminder(REMINDER1)
        database.reminderDao().saveReminder(REMINDER2)

        val expectedResult = Result.Success(database.reminderDao().getReminders())

        // when
        val result = localDataSource.getReminders() as Result.Success

        // then
        assertThat(result, `is`(expectedResult))
    }

    @Test
    fun deleteAllReminders_successfullyDeleteAllReminder() = mainCoroutineRule.runBlockingTest {
        // given
        database.reminderDao().saveReminder(REMINDER1)
        database.reminderDao().saveReminder(REMINDER2)

        val expectedResult = Result.Success(emptyList<ReminderDTO>())

        // when
        localDataSource.deleteAllReminders()

        // then
        val result = localDataSource.getReminders() as Result.Success

        assertThat(result, `is`(expectedResult))
    }
}