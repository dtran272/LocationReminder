package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: IRemindersRepository
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as IRemindersRepository
                )
            }
            single { FakeAndroidRemindersRepository() as IRemindersRepository }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun clickAddReminder_navigateToSaveReminderFragment() = runBlockingTest {
        // given
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // when
        onView(withId(R.id.addReminderFAB)).perform(click())

        // then
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun loadedRemindersList_displayRemindersListInUi() = runBlockingTest {
        // given
        repository.saveReminder(ReminderDTO("Clean room", "Remember to clean room.", "My room", 42.123, 75.3216, "1"))
        repository.saveReminder(ReminderDTO("Clean garage", "Remember to garage.", "My garage", 48.12523, 77.854531352, "2"))

        // when
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // then
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasChildCount(2)))
    }

    @Test
    fun emptyRemindersList_displayEmptyMessage() = runBlockingTest {
        // when
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // then
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasChildCount(0)))
    }
}