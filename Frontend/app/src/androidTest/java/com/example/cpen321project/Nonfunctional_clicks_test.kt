package com.example.cpen321project

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.example.cpen321project.BuildConfig.GOOGLE_REAL_TOKEN
import com.example.cpen321project.BuildConfig.GOOGLE_USER_ID
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep

class Nonfunctional_clicks_test {
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
    fun user_clicks_less_than_threshold(){
        Log.d(TAG, "Starting test: Usability for managing journal")

        val journalText =
            "I had a good balance between work and fun today. Days like this remind me why balance is so important."
        val startMessage = "Start"

        Log.d(TAG, "Test for Creating: Clicking on an unhighlighted date, Click 1")
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

        Log.d(TAG, "Clicking send chat button. Click 2")
        onView(withId(R.id.sendChatButton)).perform(click())

        sleep(1000)

        Log.d(TAG, "Typing journal entry: $journalText")
        onView(withId(R.id.chatInput))
            .perform(typeText(journalText), closeSoftKeyboard())

        Log.d(TAG, "Clicking send chat button again. Click 3")
        onView(withId(R.id.sendChatButton)).perform(click())

        sleep(5000)

        Log.d(TAG, "Done in 3 clicks. Clicking back button to entries. Now delete")
        onView(withId(R.id.Backbuttonentries)).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if calendar view is displayed")
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        Log.d(TAG, "Testing: Deleting")

        sleep(1000)

        Log.d(TAG, "Clicking on a highlighted date. Click 1")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(1000)

        Log.d(TAG, "Checking if delete button is displayed")
        onView(withId(R.id.deletebutton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking delete button, click 2")
        onView(withId(R.id.deletebutton)).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if confirmation dialog is displayed")
        onView(withText("Delete Journal Entry")).check(matches(isDisplayed()))

        sleep(1000)

        Log.d(TAG, "Clicking Yes to confirm deletion, Click 3")
        onView(allOf(withText("Yes"), isDisplayed())).perform(click())

        sleep(1000)

        Log.d(TAG, "Testing: Editing")

        val additionalText = ". I also played soccer to make my day feel even better"

        sleep(1000)

        Log.d(TAG, "Clicking on a highlighted date, Click 1")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("12")), click()
                )
            )

        sleep(1000)

        Log.d(TAG, "Checking if Save Entry button is displayed")
        onView(withId(R.id.Saveentrybutton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking edit button, Click 2")
        onView(withId(R.id.editbutton)).perform(click())

        sleep(500)

        Log.d(TAG, "Typing additional text: $additionalText")
        onView(withId(R.id.journalEntryInput)).perform(
            typeText(additionalText),
            closeSoftKeyboard()
        )

        Log.d(TAG, "Clicking Save Entry button, click 3")
        onView(withId(R.id.Saveentrybutton)).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if calendar view is displayed")
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

    }
}