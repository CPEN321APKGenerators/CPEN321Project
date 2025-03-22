package com.example.cpen321project

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.example.cpen321project.BuildConfig.GOOGLE_REAL_TOKEN
import com.example.cpen321project.BuildConfig.GOOGLE_USER_ID
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep

class AnalyticsTest {
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
    fun User_Analytics_check_emotions() {
        Log.d(TAG, "Starting test: User_Analytics_check_emotions")

        Log.d(TAG, "Clicking on Analytics button")
        onView(withId(R.id.analytics_button)).perform(click())

        Log.d(TAG, "Waiting for Analytics screen to load")
        sleep(10000)

        Log.d(TAG, "Checking if emotion filter button is displayed")
        onView(withId(R.id.emotionFilterButton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking on emotion filter button")
        onView(withId(R.id.emotionFilterButton)).perform(click())

        Log.d(TAG, "Selecting emotions: Joy and Sadness")
        onView(withText("Joy")).perform(click())
        onView(withText("Sadness")).perform(click())

        Log.d(TAG, "Clicking Apply button")
        onView(withText("Apply")).perform(click())

        Log.d(TAG, "Checking if analytics chart is displayed")
        onView(withId(R.id.analyticsChart)).check(matches(isDisplayed()))

        Log.d(TAG, "Test User_Analytics_check_emotions completed successfully")
    }

    @Test
    fun User_Analytics_check_activities() {
        Log.d(TAG, "Starting test: User_Analytics_check_activities")

        Log.d(TAG, "Clicking on Analytics button")
        onView(withId(R.id.analytics_button)).perform(click())

        Log.d(TAG, "Waiting for Analytics screen to load")
        sleep(10000)

        Log.d(TAG, "Checking if activity filter button is displayed")
        onView(withId(R.id.activityfilterButton)).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking on activity filter button")
        onView(withId(R.id.activityfilterButton)).perform(click())

        sleep(1000)

        Log.d(TAG, "Selecting activities: Sleep and Walk")
        onView(withText("sleep")).perform(click())
        onView(withText("walk")).perform(click())

        Log.d(TAG, "Clicking Apply button")
        onView(withText("Apply")).perform(click())

        sleep(1000)

        Log.d(TAG, "Checking if activities chart is displayed")
        onView(withId(R.id.activities_chart)).check(matches(isDisplayed()))

        Log.d(TAG, "Test User_Analytics_check_activities completed successfully")
    }
}
