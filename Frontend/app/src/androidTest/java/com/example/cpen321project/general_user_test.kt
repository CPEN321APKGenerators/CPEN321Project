package com.example.cpen321project

import android.content.Context
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TimePicker
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import com.example.cpen321project.BuildConfig.GOOGLE_REAL_TOKEN
import com.example.cpen321project.BuildConfig.GOOGLE_USER_ID


@RunWith(AndroidJUnit4::class)
class ProfileManagementTest {

    @get:Rule
//    val activityRule = ActivityTestRule(ProfileManagement::class.java, false, false)
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    private val TAG = "EspressoTest"

    @Before
    fun setup() {
        // Set up valid authentication state
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyToken = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030

        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
            .putString("GoogleUserID", GOOGLE_USER_ID)
            .putString("GoogleIDtoken", GOOGLE_REAL_TOKEN)
            .apply()

        // Launch activity
        Log.d(TAG, "Launching main activity")
        activityRule.launchActivity(null)
        Intents.init()
    }

    @After
    fun cleanup() {
        Log.d(TAG, "Releasing intents")
        Intents.release()
    }

    @Test
    fun testReminderSettings() {
        Log.d(TAG, "Starting test: Reminder Settings")

        Thread.sleep(2000)
        Log.d(TAG, "Clicking on profile button")
        onView(withId(R.id.profile_button)).perform(click())

        Thread.sleep(2000)

        val (hour, minute) = getCurrentTimePlusOneMinute()
        val weekdayId = getCurrentWeekdayId()

        Log.d(TAG, "Verifying current reminder settings")
        try {
            onView(withId(weekdayId))
                .check(ViewAssertions.matches(hasBackground(R.drawable.circle_purple)))
        } catch (e: AssertionError) {
            Log.d(TAG, "Selecting weekday for reminder")
            onView(withId(weekdayId)).perform(click())
        }

        Log.d(TAG, "Setting reminder time to $hour:$minute")
        setTime(hour, minute)

        Log.d(TAG, "Saving reminder settings")
        onView(withId(R.id.save_settings_button)).perform(click())

        Thread.sleep(1000)
        Log.d(TAG, "Verifying success toast message")
        onView(withText("Reminder updated successfully!"))
            .inRoot(ToastMatcher().apply { matches(isDisplayed()) })

        Log.d(TAG, "Verifying selected weekday remains highlighted")
        onView(withId(weekdayId))
            .check(ViewAssertions.matches(hasBackground(R.drawable.circle_purple)))

        Log.d(TAG, "Waiting for notification...")
        Thread.sleep(60000)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        Thread.sleep(2000)

        val notificationText = device.findObject(UiSelector().textContains("Journal Reminder"))
        assert(notificationText.exists()) { "Notification was not found!" }

        Log.d(TAG, "Notification verified successfully")
        device.pressBack()

        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500)
    }

    @Test
    fun testActivityListManagement() {
        Log.d(TAG, "Starting test: Activity List Management")

        Thread.sleep(2000)
        Log.d(TAG, "Clicking on profile button")
        onView(withId(R.id.profile_button)).perform(click())

        Thread.sleep(2000)
        val original_activities_num = getListViewSize()
        Log.d(TAG, "Original activity count: $original_activities_num")

        Log.d(TAG, "Adding new activity: Running")
        onView(withId(R.id.profile_add_activity_button)).perform(click())
        Thread.sleep(500)

        onView(withHint("Enter activity name")).perform(typeText("Running"), closeSoftKeyboard())
        Thread.sleep(500)

        onView(withHint("Enter average value")).perform(typeText("30"), closeSoftKeyboard())
        Thread.sleep(500)

        Log.d(TAG, "Selecting 'Minutes' from dropdown")
        onView(withClassName(Matchers.equalTo(Spinner::class.java.name))).perform(click())
        onData(Matchers.equalTo("Minutes"))
            .inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        Thread.sleep(1000)

        Log.d(TAG, "Confirming addition")
        onView(withText("Add")).perform(click())
        Thread.sleep(2000)

        Log.d(TAG, "Verifying new activity exists in the list")
        onData(Matchers.anything())
            .inAdapterView(withId(R.id.profile_activity_list))
            .atPosition(original_activities_num)
            .check(matches(hasDescendant(withText("Running"))))

        Log.d(TAG, "Deleting activity: Running")
        onData(Matchers.anything())
            .inAdapterView(withId(R.id.profile_activity_list))
            .atPosition(original_activities_num)
            .perform(longClick())
        onView(withText("Delete")).perform(click())
        Thread.sleep(500)

        Log.d(TAG, "Saving settings")
        onView(withId(R.id.save_settings_button)).perform(click())
        Thread.sleep(1000)

        Log.d(TAG, "Verifying success toast message")
        onView(withText("Profile updated successfully!"))
            .inRoot(ToastMatcher().apply { matches(isDisplayed()) })

        Log.d(TAG, "Test completed successfully")
    }

    @Test
    fun testPreferredName() {
        Log.d(TAG, "Starting test: Update Preferred Name")

        Thread.sleep(2000)
        Log.d(TAG, "Clicking on profile button")
        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        val newName = "John Doe"
        Log.d(TAG, "Entering new preferred name: $newName")
        onView(withId(R.id.profile_name_input))
            .perform(clearText(), typeText(newName), closeSoftKeyboard())

        Log.d(TAG, "Saving name update")
        onView(withId(R.id.save_settings_button)).perform(click())

        Thread.sleep(1000)
        Log.d(TAG, "Verifying success toast message")
        onView(withText("Profile updated successfully!"))
            .inRoot(ToastMatcher().apply { matches(isDisplayed()) })

        Log.d(TAG, "Navigating back and reopening profile to verify change")
        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500)
        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        Log.d(TAG, "Verifying preferred name is updated")
        onView(withId(R.id.profile_name_input))
            .check(matches(withText(newName)))

        Log.d(TAG, "Test completed successfully")
    }


    private fun setTime(hour: Int, minute: Int) {
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(
            PickerActions.setTime(hour, minute)
        )
    }

    // Custom matchers
    private fun hasBackground(resId: Int): Matcher<in View>? = object : BoundedMatcher<View, ImageView>(ImageView::class.java) {
        override fun matchesSafely(imageView: ImageView) =
            imageView.background.constantState?.equals(
                imageView.context.getDrawable(resId)?.constantState
            ) ?: false

        override fun describeTo(description: Description) {
            description.appendText("has background resource $resId")
        }
    }

    private fun hasListSize(size: Int): Matcher<View> {
        return object : BoundedMatcher<View, ListView>(ListView::class.java) {
            override fun matchesSafely(view: ListView) = view.adapter?.count == size
            override fun describeTo(description: Description) {
                description.appendText("ListView should have $size items")
            }
        }
    }

    fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            var currentIndex = 0

            override fun matchesSafely(view: View): Boolean {
                return matcher.matches(view) && currentIndex++ == index
            }

            override fun describeTo(description: Description) {
                description.appendText("with index: $index ")
                matcher.describeTo(description)
            }
        }
    }

    fun getListViewSize(): Int {
        var itemCount = 0
        onView(withId(R.id.profile_activity_list))
            .check { view, _ ->
                val listView = view as android.widget.ListView
                itemCount = listView.adapter?.count ?: 0
            }
        return itemCount
    }

    fun getCurrentTimePlusOneMinute(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, 1) // Add 1 minute
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return Pair(hour, minute)
    }

    fun getCurrentWeekdayId(): Int {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> R.id.day_sun
            Calendar.MONDAY -> R.id.day_mon
            Calendar.TUESDAY -> R.id.day_tue
            Calendar.WEDNESDAY -> R.id.day_wed
            Calendar.THURSDAY -> R.id.day_thu
            Calendar.FRIDAY -> R.id.day_fri
            Calendar.SATURDAY -> R.id.day_sat
            else -> throw IllegalStateException("Invalid day of the week")
        }
    }

    fun waitForToast(messages: List<String>, timeout: Long = 5000) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val endTime = System.currentTimeMillis() + timeout

        do {
            for (message in messages) {
                val toast = device.findObject(UiSelector().textContains(message))
                if (toast.exists()) {
                    return // Toast found, exit function
                }
            }
            Thread.sleep(500) // Wait and retry
        } while (System.currentTimeMillis() < endTime)

        throw AssertionError("None of the expected Toast messages were found: $messages")
    }

    class ToastMatcher : TypeSafeMatcher<Root?>() {

        override fun matchesSafely(item: Root?): Boolean {
            val type: Int? = item?.windowLayoutParams?.get()?.type
            if (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW) {
                val windowToken: IBinder = item.decorView.windowToken
                val appToken: IBinder = item.decorView.applicationWindowToken
                if (windowToken === appToken) { // means this window isn't contained by any other windows.
                    return true
                }
            }
            return false
        }

        override fun describeTo(description: Description?) {
            description?.appendText("is toast")
        }
    }

}