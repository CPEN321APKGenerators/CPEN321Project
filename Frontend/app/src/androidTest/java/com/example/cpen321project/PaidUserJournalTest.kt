package com.example.cpen321project

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PaidUserJournalTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun A_User_Upload_Image_popup_check_camera(){
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)

        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        onView(withId(R.id.addimageButton)).perform(click())
        onView(withText("Upload Media")).check(matches(isDisplayed()))
        onView(withText("Take a Photo")).perform(click())

    }

    @Test
    fun B_User_Upload_Image_popup_check_device(){
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)

        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        onView(withId(R.id.addimageButton)).perform(click())
        onView(withText("Upload Media")).check(matches(isDisplayed()))
        onView(withText("Select from Gallery")).perform(click())

    }
    @Test
    fun C_User_Deletes_existing_Image_popup_check(){
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("12")), click()
                )
            )
        sleep(1000)

        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        onView(withId(R.id.journalImageView)).perform(click())
        onView(withText("Delete Image")).check(matches(isDisplayed()))
        sleep(1000)
        onView(withText("Delete")).perform(click())
    }
}