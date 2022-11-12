package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeRemindersRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private val reminder1 = ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1")
    private val reminder2 = ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2")
    private val reminder3 = ReminderDTO("Clean kitchen", "Remember to kitchen.", "My kitchen", 46.1233251, 71.2133216, "3")
    private val reminder4 = ReminderDTO("Clean living room", "Remember to living room.", "My living room", 40.325136123, 79.3235416, "4")

    private lateinit var remindersRepository: FakeRemindersRepository

    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        remindersRepository = FakeRemindersRepository()
        remindersRepository.addReminders(reminder1, reminder2, reminder3, reminder4)

        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), remindersRepository)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_getRemindersException_shouldReturnError() = mainCoroutineRule.runBlockingTest {
        // given
        remindersRepository.setReturnError(true)
        mainCoroutineRule.pauseDispatcher()

        // when
        viewModel.loadReminders()

        // then
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_shouldReturnReminders() = mainCoroutineRule.runBlockingTest {
        // given
        mainCoroutineRule.pauseDispatcher()

        // when
        viewModel.loadReminders()

        // then
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.remindersList.getOrAwaitValue().size, `is`(4))
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(false))
    }
}