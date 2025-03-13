package com.example.cpen321project

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PaidUserJournalTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(Journal_entries::class.java)

    @Test
    fun A_User_Upload_Image(){

    }

    @Test
    fun B_User_Deletes_Image(){

    }
}