package com.example.cpen321project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

import java.time.ZoneId
import java.time.ZonedDateTime

class MainActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var selectedDate: LocalDate
    private val journalentries = mutableSetOf<String>()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is authenticated
        val googleUserId = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)

        if (googleUserId.isNullOrEmpty()) {
            // If not authenticated, redirect to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Prevents going back to MainActivity without authentication
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get FCM Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                Log.d("FCM Token", fcmToken ?: "No Token")

                // Store Token in Backend
                storeFcmTokenInBackend(fcmToken ?: "", userID = googleUserId)
            }
        }

        val googleUserIdd = intent.getStringExtra("GoogleUserID") ?:
        getSharedPreferences("AppPreferences", MODE_PRIVATE).getString("GoogleUserID", null)
        Log.d("MainActivity", "Google User ID: $googleUserId")

        // need to get the actual date from the back end just for example
        journalentries.add("2025-02-05")
        journalentries.add("2025-02-10")
        val adddate = intent.getStringExtra("added_date") ?: ""
        journalentries.add(adddate)
        val deletedate = intent.getStringExtra("deleted_date") ?: ""
        journalentries.remove(deletedate)

        initWidgets()
        selectedDate = LocalDate.now()
        setMonthView()

        findViewById<Button>(R.id.Last_month_button).setOnClickListener(){
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.Next_month_button).setOnClickListener(){
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.profile_button).setOnClickListener() {
            val intent = Intent(this, ProfileManagement::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.log_out_button).setOnClickListener() {
            logOut()
        }


    }

    private fun initWidgets() {
        calendarRecyclerView = findViewById(R.id.calenderrecycleView)
        monthYearText = findViewById(R.id.Year_month_TV)
    }

    private fun setMonthView() {
        monthYearText.text = monthYearFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(daysInMonth, selectedDate, journalentries,this)
        val layoutManager = GridLayoutManager(applicationContext,7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)

        val daysInMonth = yearMonth.lengthOfMonth()
        val firstofMonth = selectedDate.withDayOfMonth(1)

        val dayOfWeek = firstofMonth.dayOfWeek.value

        for(i in 1..42){
            if(i<= dayOfWeek || i> daysInMonth + dayOfWeek){
                daysInMonthArray.add("")
            } else{
                daysInMonthArray.add((i-dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }

    private fun monthYearFromDate(date: LocalDate): CharSequence? {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        return date.format(formatter)
    }

    override fun onItemClick(position: Int, dayText: String) {
        if (dayText.isNotEmpty()) {
            val message = "Selected Date $dayText ${monthYearFromDate(selectedDate)}"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            val dayNumber = dayText.toInt()
            val selectedJournalDate = selectedDate.withDayOfMonth(dayNumber)

            // Allow only past or current dates
            if (!selectedJournalDate.isAfter(LocalDate.now())) {
                val intent = Intent(this, Journal_entries::class.java)
                intent.putExtra("SELECTED_DATE", selectedJournalDate.toString()) // Pass date to next activity
                if(journalentries.contains(selectedJournalDate.toString())){ // fetch the journal entries that have been written from the database
                    val entry:String = "Fetched from database"
                    intent.putExtra("Journal_Entry_fetched", entry)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Cannot add a journal for future dates!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun storeFcmTokenInBackend(fcmToken: String, userID: String) {
        Log.d(TAG, "storing fcm token for ${userID}")
//        val userID = "12345" // Get this dynamically (e.g., after user login)
        val url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile/fcmtoken"
//        val url = "http://10.0.2.2:3001/api/profile/fcmtoken"
        val currentZone = ZoneId.systemDefault()
        val zonedDateTime = ZonedDateTime.now(currentZone)
        val offset = zonedDateTime.offset
        val json = JSONObject()
        Log.d("time zone", "$currentZone, $zonedDateTime, $offset")
        json.put("userID", userID)
        json.put("fcmToken", fcmToken)
        json.put("timeOffset", offset)

        val client = OkHttpClient()
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), json.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("FCM Token", "Token stored successfully")
                    Log.d("FCM Token", "Response: ${response.code} - ${response.message}")

                    // Safely read response body as String
                    val responseBody = response.body?.string() ?: "No Response Body"
                    Log.d("FCM Token", "Response Body: $responseBody")
                } else {
                    Log.e("FCM Token", "Failed to store token. Status Code: ${response.code}")
                    val responseBody = response.body?.string() ?: "No Response Body"
                    Log.e("FCM Token", "Response Body: $responseBody")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM Token", "Failed to store token", e)
            }
        })
    }

    fun logOut() {
        // Clear Google User ID from SharedPreferences
        getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .edit()
            .remove("GoogleUserID")
            .apply()

        // Redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

}
