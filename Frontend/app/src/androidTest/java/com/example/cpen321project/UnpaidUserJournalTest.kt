package com.example.cpen321project

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
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

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)


    @Test
    fun A_User_click_on_unhiglighted_create_entry() {
        val journalText =
            "I had a good balance between work and fun today. Days like this remind me why balance is so important."
        val startMessage = "Start"
        // Click on an unhighlighted date
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        onView(withId(R.id.chatInput)).check(matches(isDisplayed()))

        onView(withId(R.id.chatInput))
            .perform(typeText(startMessage), closeSoftKeyboard())
        onView(withId(R.id.sendChatButton))
            .perform(click())
        sleep(1000)
        onView(withId(R.id.chatInput))
            .perform(typeText(journalText), closeSoftKeyboard())
        onView(withId(R.id.sendChatButton))
            .perform(click())
        sleep(5000)

        onView(withId(R.id.Backbuttonentries)).perform(click())
        sleep(1000)
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"), // Check the specific date
                            withBackground(R.drawable.circle_background) // Check if it has the highlighted background
                        )
                    )
                )
            )
    }

    @Test
    fun B_User_click_on_higlighted_edit_entry() {
        val additional_text = ". I also played soccer to make my day feel even better"
        // Click on an highlighted date
        sleep(1000)
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)
        onView(withId(R.id.Saveentrybutton)).check(matches(isDisplayed()))

        onView(withId(R.id.editbutton)).perform(click())
        sleep(500)

        onView(withId(R.id.journalEntryInput)).perform(
            typeText(additional_text),
            closeSoftKeyboard()
        )

        onView(withId(R.id.Saveentrybutton)).perform(click())
        sleep(1000)

        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"), // Check the specific date
                            (withBackground(R.drawable.circle_background))// Check if it has the highlighted background
                        )
                    )
                )
            )
    }

    @Test
    fun C_User_click_on_higlighted_delete_entry() {
        // Click on an highlighted date
        sleep(1000)
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(1000)
        onView(withId(R.id.deletebutton)).check(matches(isDisplayed()))

        onView(withId(R.id.deletebutton)).perform(click())
        sleep(1000)
        onView(withText("Delete Journal Entry")).check(matches(isDisplayed()))
        sleep(1000)
        onView(allOf(withText("Yes"), isDisplayed())).perform(click())
        sleep(1000)
        onView(withId(R.id.calenderrecycleView)).check(matches(isDisplayed()))

        onView(withId(R.id.calenderrecycleView))
            .check(
                matches(
                    hasDescendant(
                        allOf(
                            withText("1"), // Check the specific date
                            not(withBackground(R.drawable.circle_background))// Check if it has the highlighted background
                        )
                    )
                )
            )
    }

    @Test
    fun D_User_clicks_future_date() {
        onView(withId(R.id.Next_month_button)).perform(click())
        sleep(1000)
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )

        sleep(1000)
        // Check if a Toast with expected message is displayed
        onView(withText("Cannot add a journal for future dates!"))
            .inRoot(ToastMatcher().apply{
                matches(isDisplayed())
            })
    }

    @Test
    fun E_User_cannot_upload_image() {
        onView(withId(R.id.calenderrecycleView))
            .perform(
                RecyclerViewActions.actionOnItem<CalendarAdapter.ViewHolder>(
                    hasDescendant(withText("1")), click()
                )
            )
        sleep(1000)
        onView(withId(R.id.addimageButton))
            .perform(click())

        sleep(1000)
        onView(withText("Upgrade to upload media!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })
    }

    @Test
    fun F_User_export_journal() {
        onView(withId(R.id.export_button)).check(matches(isDisplayed()))
        onView(withId(R.id.export_button)).perform(click())

        sleep(1000)

        onView(withText("File URL copied to clipboard!"))
            .inRoot(ToastMatcher().apply {
                matches(isDisplayed())
            })
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
