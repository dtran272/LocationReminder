package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {
    private val REMINDER1 = ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1")
    private val REMINDER2 = ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2")

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getReminders_returnsExpectedNumberOfReminders() = runBlockingTest {
        // given
        database.reminderDao().saveReminder(REMINDER1)
        database.reminderDao().saveReminder(REMINDER2)

        // when
        val result = database.reminderDao().getReminders()

        // then
        assertThat(result.size, `is`(2))
    }

    @Test
    fun getReminderById_returnsExpectedReminder() = runBlockingTest {
        // given
        database.reminderDao().saveReminder(REMINDER1)
        database.reminderDao().saveReminder(REMINDER2)

        // when
        val result = database.reminderDao().getReminderById(REMINDER1.id)

        // then
        assertThat(result, `is`(REMINDER1))
    }

    @Test
    fun saveReminder_savesNewReminder() = runBlockingTest {
        // when
        database.reminderDao().saveReminder(REMINDER1)

        // then
        val result = database.reminderDao().getReminders()

        assertThat(result.size, `is`(1))
        assertThat(result.first(), `is`(REMINDER1))
    }

    @Test
    fun deleteAllReminders_returnsZeroSize() = runBlockingTest {
        // given
        database.reminderDao().saveReminder(REMINDER1)
        database.reminderDao().saveReminder(REMINDER2)

        // when
        database.reminderDao().deleteAllReminders()

        // then
        val result = database.reminderDao().getReminders()
        assertThat(result.size, `is`(0))
    }
}