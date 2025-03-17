package com.example.cpen321project

import android.content.Context
import android.os.IBinder
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


@RunWith(AndroidJUnit4::class)
class ProfileManagementTest {

    @get:Rule
//    val activityRule = ActivityTestRule(ProfileManagement::class.java, false, false)
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)


    @Before
    fun setup() {
        // Set up valid authentication state
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyToken = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030
        val realToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjkxNGZiOWIwODcxODBiYzAzMDMyODQ1MDBjNWY1NDBjNmQ0ZjVlMmYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI1ODUwNDAyMDQyMTAtMmE3ZW9hbjF1YnM3aGJjZWRyY24zb2xyZHJnN2dyMDAuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI1ODUwNDAyMDQyMTAtamxscW8ybjNvZHJmcmY4dGhiZ3ZoaXY2azFwYzVmMmcuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI3NjgzMjIyNzA1ODAzNzA2OTkiLCJlbWFpbCI6ImxsY2NlNDRAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJDaHJpc3RpbmUgSklBTkciLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSXVLSEZJZUpSVXZpeGV3Z2Z1UXRBZXRoUXNiMnV0VFY5MWNXbG1vcXdUWExQTGFnPXM5Ni1jIiwiZ2l2ZW5fbmFtZSI6IkNocmlzdGluZSIsImZhbWlseV9uYW1lIjoiSklBTkciLCJpYXQiOjE3NDE4OTIxMjUsImV4cCI6MTc0MTg5NTcyNX0.3dOUctz1Zj8TzZ8b2T945MTaT9DNGgzo0W-kHxks-pbWtCWkir0pJmsSn-urkqe8YY1cbzKGZg1rXivgdPavSpY0fKedlVp2R-AYI6r0TeLhxqrQS7Tr4is7fTxuZifTszwLml2Hmv9w9NvDAMJo8NnyPLWRHII-Kxu46mQRFHp0sQpsByVrSfpAuylYKygLI0wdXnyFjL-5NWHTdXJ_rXEmxIVlyDwFTxYulmoX4Aza9rEtj1aNiqvOq3jOdsKBKxbIZfGAgPkwVcSzAXbEsO5_3LaKkYylWZCcs83N3QAhaNEYA-yDxtrm7Ies-DxvGpfJU9s0Mnpso5kR79CYmw"

        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
            .putString("GoogleUserID", "llcce44@gmail.com")
            .putString("GoogleIDtoken", realToken)
            .apply()

        // Launch activity
        activityRule.launchActivity(null)
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun testReminderSettings() {
        // Wait for initial load
        Thread.sleep(2000)

        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        // Get current time + 1 minute
        val (hour, minute) = getCurrentTimePlusOneMinute()
        val weekdayId = getCurrentWeekdayId()

        try {
            onView(withId(weekdayId))
                .check(ViewAssertions.matches(hasBackground(R.drawable.circle_purple)))
            // If the background is purple, do nothing
        } catch (e: AssertionError) {
            // If the background is NOT purple, Select today's weekday
            onView(withId(weekdayId)).perform(click())
        }

        // Set time dynamically
        setTime(hour, minute)

        // Save settings
        onView(withId(R.id.save_settings_button)).perform(click())
        Thread.sleep(1000) // Wait for save
        // Check for each toast using their exact text
        onView(withText("Reminder updated successfully!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            });

        // Verify day remain selected
        onView(withId(weekdayId))
            .check(ViewAssertions.matches(hasBackground(R.drawable.circle_purple)))

        // **Wait 1 minute for the notification**
        Thread.sleep(60000) // 60 seconds

        // Use UiAutomator to open and verify the notification
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.openNotification()
        Thread.sleep(2000) // Give some time for notifications to load

        val notificationText = device.findObject(UiSelector().textContains("Journal Reminder"))
        assert(notificationText.exists()) { "Notification was not found!" }

        device.pressBack()

        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500)
    }

    @Test
    fun testActivityListManagement() {
        // Wait for initial load
        Thread.sleep(2000)

        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)
        val original_activities_num = getListViewSize()

        // Add new activity
        onView(withId(R.id.profile_add_activity_button)).perform(click())
        Thread.sleep(500)

        // Fill in dialog
        onView(withHint("Enter activity name"))
            .perform(typeText("Running"), closeSoftKeyboard())
        Thread.sleep(500)

        onView(withHint("Enter average value"))
            .perform(typeText("30"), closeSoftKeyboard())
        Thread.sleep(500)

        // Select "Minutes" from spinner
        onView(withClassName(Matchers.equalTo(Spinner::class.java.name)))
            .perform(click())
        onData(Matchers.equalTo("Minutes"))  // Find the item in the dropdown list
            .inRoot(RootMatchers.isPlatformPopup()) // Ensure it's inside the dropdown
            .perform(click())
        Thread.sleep(1000)


        // Confirm addition
        onView(withText("Add")).perform(click())
        Thread.sleep(2000)

        onData(Matchers.anything())
            .inAdapterView(withId(R.id.profile_activity_list))
            .atPosition(0)
            .check(matches(isDisplayed())) // Ensures the item exists

        // Verify activity in list
        onData(Matchers.anything())
            .inAdapterView(withId(R.id.profile_activity_list))
            .atPosition(original_activities_num)  // Check last position
            .check(matches(hasDescendant(withText("Running"))))

        // Delete the activity
        onData(Matchers.anything())
            .inAdapterView(withId(R.id.profile_activity_list))
            .atPosition(original_activities_num)
            .perform(longClick())
        onView(withText("Delete")).perform(click())
        Thread.sleep(500)

        // Verify list empty
        onView(withId(R.id.profile_activity_list))
            .check(ViewAssertions.matches(hasListSize(original_activities_num)))

        onView(withId(R.id.save_settings_button)).perform(click())
        Thread.sleep(1000)
        // check toast
        onView(withText("Profile updated successfully!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })
        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500)
    }

    @Test
    fun testPreferredName() {
        // Wait for initial load
        Thread.sleep(2000)

        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        val newName = "John Doe"
        onView(withId(R.id.profile_name_input))
            .perform(clearText(), typeText(newName), closeSoftKeyboard())

        // Click Save button
        onView(withId(R.id.save_settings_button)).perform(click())
        Thread.sleep(1000)

        // Wait for Toast confirmation
        onView(withText("Profile updated successfully!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })

        // back and re-enter activity to verify update
        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500) // Wait for the UI to reload
        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        // Verify preferred name is updated
        onView(withId(R.id.profile_name_input))
            .check(matches(withText(newName)))

        onView(withId(R.id.profile_back_button)).perform(click())
        Thread.sleep(500)
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