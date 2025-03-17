package com.example.cpen321project

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep

class AnalyticsTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val TAG = "EspressoTest"

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
        sleep(20000)

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
