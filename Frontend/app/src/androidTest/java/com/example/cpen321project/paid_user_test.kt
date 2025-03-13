package com.example.cpen321project

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class paid_user_test {

    // Manually control when the activity is launched
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Before
    fun setup() {
        // Set up valid authentication state in SharedPreferences
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyToken = "eyJhbGciOiJub25lIn0.eyJleHAiOjE4OTM0NTYwMDB9." // Expires in 2030

        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
            .putString("GoogleUserID", "llcce44@gmail.com")
            .putString("GoogleIDtoken", dummyToken)
            .apply()

        // Launch activity after setting up preferences
        activityRule.launchActivity(null)
    }

    @Test
    fun viewUserPaidProfile() {
        // Wait for initial loading (adjust as needed)
        Thread.sleep(2000)

        onView(withId(R.id.profile_button)).perform(click())
        Thread.sleep(2000)

        onView(withId(R.id.profile_upgrade_button))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}