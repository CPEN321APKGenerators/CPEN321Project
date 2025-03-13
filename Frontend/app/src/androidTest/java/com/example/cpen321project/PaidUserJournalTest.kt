package com.example.cpen321project

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class PaidUserJournalTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }
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

        // Mock the camera response
        val resultData = Intent()
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        resultData.putExtra("data", bitmap)

        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(result)

        // Check if the image is displayed in ImageView
        onView(withId(R.id.journalImageView)).check(matches(isDisplayed()))
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