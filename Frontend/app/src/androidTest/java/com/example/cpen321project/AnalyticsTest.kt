package com.example.cpen321project

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

    @Test
    fun User_Analytics_check_emotions(){
        onView(withId(R.id.analytics_button)).perform(click())
        sleep(1000)
        onView(withId(R.id.emotionFilterButton)).check(matches(isDisplayed()))

        onView(withId(R.id.emotionFilterButton)).perform(click())

        onView(withText("Joy")).perform(click())
        onView(withText("Sadness")).perform(click())

        // Step 3: Click "Apply" to update the chart
        onView(withText("Apply")).perform(click())

        // Step 4: Verify that the chart is updated (by checking if the view is displayed)
        onView(withId(R.id.analyticsChart))
            .check(matches(isDisplayed()))
    }

    @Test
    fun User_Analytics_check_activities(){
        onView(withId(R.id.analytics_button)).perform(click())
        sleep(1000)
        onView(withId(R.id.activityfilterButton)).check(matches(isDisplayed()))

        onView(withId(R.id.activityfilterButton)).perform(click())
        sleep(1000)

        onView(withText("Sleep")).perform(click())
        onView(withText("Sadness")).perform(click())

        // Step 3: Click "Apply" to update the chart
        onView(withText("Apply")).perform(click())

        sleep(1000)
        // Step 4: Verify that the chart is updated (by checking if the view is displayed)
        onView(withId(R.id.activities_chart))
            .check(matches(isDisplayed()))
    }
}