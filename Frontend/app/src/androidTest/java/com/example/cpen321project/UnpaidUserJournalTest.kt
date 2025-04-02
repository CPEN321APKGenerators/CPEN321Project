package com.example.cpen321project

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.example.cpen321project.BuildConfig.GOOGLE_REAL_TOKEN
import com.example.cpen321project.BuildConfig.GOOGLE_USER_ID
import com.example.cpen321project.calendar.CalendarAdapter
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UnpaidUserJournalTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    private val TAG = "EspressoTest"

    @Before
    fun setup() {
        // Set up valid authentication state
        val context = ApplicationProvider.getApplicationContext<Context>()

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
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun A_User_click_on_unhiglighted_create_entry() {
        Log.d(TAG, "Starting test: A_User_click_on_unhiglighted_create_entry")

        val journalText =
            "I had a good balance between work and fun today. Days like this remind me why balance is so important."
        val startMessage = "Start"

        Log.d(TAG, "Clicking on an unhighlighted date")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        Log.d(TAG, "Checking if chat input is displayed")
        onView(withId(R.id.chatInput)).check(matches(isDisplayed()))

        Log.d(TAG, "Typing start message: $startMessage")
        onView(withId(R.id.chatInput))
            .perform(typeText(startMessage), closeSoftKeyboard())

        Log.d(TAG, "Clicking send chat button")
        onView(withId(R.id.sendChatButton)).perform(click())

        sleep(5000)

        Log.d(TAG, "Typing journal entry: $journalText")
        onView(withId(R.id.chatInput))
            .perform(typeText(journalText), closeSoftKeyboard())

        Log.d(TAG, "Clicking send chat button again")
        onView(withId(R.id.sendChatButton)).perform(click())

        sleep(5000)

        Log.d(TAG, "Clicking back button to entries")
        onView(withId(R.id.Backbuttonentries)).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if calendar view is displayed")
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        Log.d(TAG, "Checking if the selected date is highlighted")
        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"),
                            withBackground(R.drawable.circle_background)
                        )
                    )
                )
            )

        Log.d(TAG, "Test A_User_click_on_unhiglighted_create_entry completed successfully")
    }

    @Test
    fun B_User_click_on_higlighted_edit_entry() {
        Log.d(TAG, "Starting test: B_User_click_on_higlighted_edit_entry")

        val additionalText = ". I also played soccer to make my day feel even better"

        sleep(1000)

        Log.d(TAG, "Clicking on a highlighted date")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(3000)

        Log.d(TAG, "Checking if Save Entry button is displayed")
        onView(withId(R.id.Saveentrybutton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking edit button")
        onView(withId(R.id.editbutton)).perform(click())

        sleep(1000)

        Log.d(TAG, "Typing additional text: $additionalText")
        onView(withId(R.id.journalEntryInput)).perform(
            typeText(additionalText),
            closeSoftKeyboard()
        )

        Log.d(TAG, "Clicking Save Entry button")
        onView(withId(R.id.Saveentrybutton)).perform(click())

        sleep(2000)

        Log.d(TAG, "Checking if calendar view is displayed")
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        Log.d(TAG, "Verifying if the date is still highlighted")
        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"),
                            withBackground(R.drawable.circle_background)
                        )
                    )
                )
            )

        Log.d(TAG, "Test B_User_click_on_higlighted_edit_entry completed successfully")
    }

    @Test
    fun C_User_click_on_higlighted_delete_entry() {
        Log.d(TAG, "Starting test: C_User_click_on_higlighted_delete_entry")

        sleep(2000)

        Log.d(TAG, "Clicking on a highlighted date")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(2000)

        Log.d(TAG, "Checking if delete button is displayed")
        onView(withId(R.id.deletebutton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking delete button")
        onView(withId(R.id.deletebutton)).perform(click())

        sleep(2000)

        Log.d(TAG, "Checking if confirmation dialog is displayed")
        onView(withText("Delete Journal Entry")).check(matches(isDisplayed()))

        sleep(2000)

        Log.d(TAG, "Clicking Yes to confirm deletion")
        onView(allOf(withText("Yes"), isDisplayed())).perform(click())

        sleep(2000)

        Log.d(TAG, "Verifying if the date is no longer highlighted")
        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"),
                            not(withBackground(R.drawable.circle_background))
                        )
                    )
                )
            )

        Log.d(TAG, "Test C_User_click_on_higlighted_delete_entry completed successfully")
    }

    @Test
    fun D_User_clicks_future_date() {
        Log.d(TAG, "Starting test: D_User_clicks_future_date")

        onView(withId(R.id.Next_month_button)).perform(click())
        sleep(1000)

        Log.d(TAG, "Clicking on a future date")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(1000)

        Log.d(TAG, "Checking if toast message is displayed")
        onView(withText("Cannot add a journal for future dates!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })

        Log.d(TAG, "Test D_User_clicks_future_date completed successfully")
    }

    @Test
    fun E_User_cannot_upload_image() {
        Log.d(TAG, "Starting test: E_User_cannot_upload_image")

        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(1000)

        Log.d(TAG, "Clicking add image button")
        onView(withId(R.id.addimageButton))
            .perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if upgrade toast message is displayed")
        onView(withText("Upgrade to upload media!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })

        Log.d(TAG, "Test E_User_cannot_upload_image completed successfully")
    }

    @Test
    fun F_User_export_journal() {
        Log.d(TAG, "Starting test: F_User_export_journal")

        onView(withId(R.id.export_button)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking export button")
        onView(withId(R.id.export_button)).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if file copied toast message is displayed")
        onView(withText("File URL copied to clipboard!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })

        Log.d(TAG, "Test F_User_export_journal completed successfully")
    }
}

fun withBackground(@DrawableRes expectedDrawable: Int): Matcher<View> {
    return object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun matchesSafely(textView: TextView): Boolean {
            return textView.background != null &&
                    textView.background.constantState == textView.context.getDrawable(
                expectedDrawable
            )?.constantState
        }

        override fun describeTo(description: Description) {
            description.appendText("with background resource: $expectedDrawable")
        }
    }
}
