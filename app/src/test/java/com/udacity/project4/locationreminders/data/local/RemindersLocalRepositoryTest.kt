package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersLocalRepositoryTest {
    private val reminder1 = ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1")
    private val reminder2 = ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2")
    private val reminder3 = ReminderDTO("Clean kitchen", "Remember to kitchen.", "My kitchen", 46.1233251, 71.2133216, "3")
    private val reminder4 = ReminderDTO("Clean living room", "Remember to living room.", "My living room", 40.325136123, 79.3235416, "4")
    private val localReminders = listOf(reminder1, reminder2, reminder3, reminder4).sortedBy { it.id }

    private lateinit var remindersLocalDataSource: FakeDataSource

    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupRepository() {
        remindersLocalDataSource = FakeDataSource(localReminders.toMutableList())

        repository = RemindersLocalRepository(remindersLocalDataSource, Dispatchers.Main)
    }

    @Test
    fun getReminders_returnsRemindersList() = mainCoroutineRule.runBlockingTest {
        // when
        val result = repository.getReminders() as Success

        //then
        assertThat(result.data, `is`(localReminders))
    }

    @Test
    fun saveReminder_savesReminder() = mainCoroutineRule.runBlockingTest {
        // given
        val reminder = ReminderDTO("Clean dorm", "Remember to dorm.", "My dorm", 42.14323, 75.345216, "5")

        // when
        repository.saveReminder(reminder)

        // then
        val result = repository.getReminders() as Success

        assertThat(result.data.size, `is`(localReminders.size + 1))
    }

    @Test
    fun getReminder_returnsReminder() = mainCoroutineRule.runBlockingTest {
        // when
        val result = repository.getReminder(reminder1.id) as Success

        //then
        assertThat(result.data, `is`(reminder1))
    }

    @Test
    fun deleteAllReminders_clearsReminders() = mainCoroutineRule.runBlockingTest {
        // when
        repository.deleteAllReminders()

        //then
        val result = repository.getReminders() as Success

        assertThat(result.data.size, `is`(0))
    }
}