package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeRemindersRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    private val reminder1 = ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1")
    private val reminder2 = ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2")
    private val reminder3 = ReminderDTO("Clean kitchen", "Remember to kitchen.", "My kitchen", 46.1233251, 71.2133216, "3")
    private val reminder4 = ReminderDTO("Clean living room", "Remember to living room.", "My living room", 40.325136123, 79.3235416, "4")

    private lateinit var remindersRepository: FakeRemindersRepository

    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        remindersRepository = FakeRemindersRepository()
        remindersRepository.addReminders(reminder1, reminder2, reminder3, reminder4)

        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun onClear_shouldClearLiveDataObjects() {
        // when
        viewModel.onClear()

        // then
        assertThat(viewModel.reminderTitle.getOrAwaitValue(), nullValue())
        assertThat(viewModel.reminderDescription.getOrAwaitValue(), nullValue())
        assertThat(viewModel.reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
        assertThat(viewModel.selectedPOI.getOrAwaitValue(), nullValue())
        assertThat(viewModel.latitude.getOrAwaitValue(), nullValue())
        assertThat(viewModel.longitude.getOrAwaitValue(), nullValue())
    }

    @Test
    fun validateAndSaveReminder_reminderTitleIsNullOrEmpty_notSavingReminder() {
        // given
        val reminder = ReminderDataItem(null, "description", "location", 42.123, 75.3216, "5")

        // when
        viewModel.validateAndSaveReminder(reminder)

        // then
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateAndSaveReminder_locationIsNullOrEmpty_notSavingReminder() {
        // given
        val reminder = ReminderDataItem("title", "description", "", 42.123, 75.3216, "5")

        // when
        viewModel.validateAndSaveReminder(reminder)

        // then
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateAndSaveReminder_validReminder_savesReminder() {
        // given
        val reminder = ReminderDataItem("title", "description", "location", 42.123, 75.3216, "5")

        mainCoroutineRule.pauseDispatcher()

        // when
        viewModel.validateAndSaveReminder(reminder)

        // then
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
        assertThat(viewModel.navigationCommand.getOrAwaitValue(), `is`(NavigationCommand.Back as NavigationCommand))
    }

    @Test
    fun validateEnteredData_reminderTitleIsNullOrEmpty_returnsFalse() {
        // given
        val reminder = ReminderDataItem(null, "description", "location", 42.123, 75.3216, "5")

        // when
        val result = viewModel.validateEnteredData(reminder)

        // then
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_locationIsNullOrEmpty_returnsFalse() {
        // given
        val reminder = ReminderDataItem("title", "description", "", 42.123, 75.3216, "5")

        // when
        val result = viewModel.validateEnteredData(reminder)

        // then
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_validReminder_returnsTrue() {
        // given
        val reminder = ReminderDataItem("title", "description", "location", 42.123, 75.3216, "5")

        // when
        val result = viewModel.validateEnteredData(reminder)

        // then
        assertThat(result, `is`(true))
    }
}