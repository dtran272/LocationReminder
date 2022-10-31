package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.local.*
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.util.waitFor
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: IRemindersRepository
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as IRemindersRepository
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as IRemindersRepository
                )
            }
            single { AuthenticationViewModel() }
            single { FakeAndroidRemindersRepository() as IRemindersRepository }
            single { LocalDB.createRemindersDao(appContext) }
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

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun createNewReminder_displayInRemindersList() = runBlocking {
        val device = UiDevice.getInstance(getInstrumentation())
        val reminderTitle = "Pick up Jimmy!"

        // 1. Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click add reminder FAB and fill out reminder form
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText(reminderTitle), closeSoftKeyboard())
        onView(withId(R.id.reminderDescription)).perform(typeText("Remember to pick up Timmy this time..."), closeSoftKeyboard())

        // 3. Click on select location and select a POI
        onView(withId(R.id.selectLocation)).perform(click())
        onView(isRoot()).perform(waitFor(5000))

        device.click(device.displayWidth / 4, device.displayHeight / 2)

        // 4. Save POI selection
        device.wait(Until.findObject(By.text("Save").clickable(true)), 10000)
        onView(withId(R.id.saveButton)).perform(click())

        // 5. Save reminder
        device.wait(Until.findObject(By.text("Reminder Location")), 10000)
        onView(withId(R.id.saveReminder)).perform(click())

        // 6. Verify toast displayed and reminder is loaded in list
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(not(getActivity(activityScenario)?.window?.decorView))
        ).check(matches(isDisplayed()))

        onView(withText(reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasChildCount(1)))

        activityScenario.close()
    }

    @Test
    fun createReminder_emptyTitle_showSnackbarErrorMessage() = runBlocking {
        // 1. Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click add reminder FAB and try to save right away
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // 3. Should display snackbar message indicating to enter title
        onView(withText(R.string.err_enter_title)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun createReminder_noLocationSelected_showSnackbarErrorMessage() = runBlocking {
        val reminderTitle = "Pick up Jimmy!"

        // 1. Start RemindersActivity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // 2. Click add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // 4. Enter title and click save
        onView(withId(R.id.reminderTitle)).perform(typeText(reminderTitle), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())

        // 5. Should display snackbar message indicating to enter location
        onView(withText(R.string.err_select_location)).check(matches(isDisplayed()))

        activityScenario.close()
    }

    private fun getActivity(activityScenario: ActivityScenario<RemindersActivity>): Activity? {
        var activity: Activity? = null
        activityScenario.onActivity {
            activity = it
        }
        return activity
    }
}
