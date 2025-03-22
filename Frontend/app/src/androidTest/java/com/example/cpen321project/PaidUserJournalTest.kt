package com.example.cpen321project

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.example.cpen321project.BuildConfig.GOOGLE_REAL_TOKEN
import com.example.cpen321project.BuildConfig.GOOGLE_USER_ID
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
    fun A_User_Upload_Image_popup_check_camera() {
        Log.d(TAG, "Starting test: A_User_Upload_Image_popup_check_camera")

        Log.d(TAG, "Clicking on date 1 in calendar")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)

        Log.d(TAG, "Checking if add image button is displayed")
        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        Log.d(TAG, "Clicking on add image button")
        onView(withId(R.id.addimageButton)).perform(click())

        Log.d(TAG, "Checking if Upload Media popup is displayed")
        onView(withText("Upload Media")).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking on Take a Photo option")
        onView(withText("Take a Photo")).perform(click())

        sleep (5000)
        Log.d(TAG, "Test A_User_Upload_Image_popup_check_camera completed successfully")
    }

    @Test
    fun B_User_Upload_Image_popup_check_device() {
        Log.d(TAG, "Starting test: B_User_Upload_Image_popup_check_device")

        Log.d(TAG, "Clicking on date 1 in calendar")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)

        Log.d(TAG, "Checking if add image button is displayed")
        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        Log.d(TAG, "Clicking on add image button")
        onView(withId(R.id.addimageButton)).perform(click())

        Log.d(TAG, "Checking if Upload Media popup is displayed")
        onView(withText("Upload Media")).check(matches(isDisplayed()))

        Log.d(TAG, "Clicking on Select from Gallery option")
        onView(withText("Select from Gallery")).perform(click())

        sleep (5000)

        Log.d(TAG, "Test B_User_Upload_Image_popup_check_device completed successfully")
    }

    @Test
    fun C_User_Deletes_existing_Image_popup_check() {
        Log.d(TAG, "Starting test: C_User_Deletes_existing_Image_popup_check")

        Log.d(TAG, "Clicking on date 12 in calendar")
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("12")), click()
                )
            )
        sleep(1000)

        Log.d(TAG, "Checking if add image button is displayed")
        onView(withId(R.id.addimageButton)).check(matches(isDisplayed()))

        sleep(1000)

        Log.d(TAG, "Clicking on journal image view")
        onView(withId(R.id.journalImageView)).perform(click())

        Log.d(TAG, "Checking if Delete Image popup is displayed")
        onView(withText("Delete Image")).check(matches(isDisplayed()))
        sleep(1000)

        Log.d(TAG, "Clicking Delete button")
        onView(withText("Delete")).perform(click())

        Log.d(TAG, "Test C_User_Deletes_existing_Image_popup_check completed successfully")
    }
}