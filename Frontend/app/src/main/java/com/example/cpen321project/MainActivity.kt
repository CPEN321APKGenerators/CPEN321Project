package com.example.cpen321project

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cpen321project.JournalEntries.Journal_entries
import com.example.cpen321project.calendar.CalendarAdapter
import com.example.cpen321project.profile.ProfileManagement
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), CalendarAdapter.OnItemListener {
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private lateinit var selectedDate: LocalDate
    private val journalentries = mutableSetOf<String>()
    private val BASE_URL = "https://cpen321project-journal.duckdns.org"

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is authenticated
        val googleUserId = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val googleidToken = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleIDtoken", null)


        if (googleUserId.isNullOrEmpty() || (googleidToken != null && isIdTokenExpired(googleidToken))) {
            // If not authenticated or token expired, redirect to LoginActivity
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
        getFCMToken(googleUserId)
        loadJournalEntries()
        val addDate = intent.getStringExtra("added_date") ?: ""
        if (addDate.isNotEmpty()) {
            journalentries.add(addDate)
            saveJournalEntries()  // Save after updating the set
        }

        val deleteDate = intent.getStringExtra("deleted_date") ?: ""
        if (deleteDate.isNotEmpty()) {
            journalentries.remove(deleteDate)
            saveJournalEntries()  // Save after updating the set
        }

        calendarRecyclerView = findViewById(R.id.calenderrecycleView)
        monthYearText = findViewById(R.id.Year_month_TV)
        selectedDate = LocalDate.now()
        setMonthView()
        setupButtonListeners(googleidToken,googleUserId)
    }

    private fun getFCMToken(googleUserId: String?) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result
                Log.d("FCM Token", fcmToken ?: "No Token")

                // Store Token in Backend
                if (googleUserId != null) {
                    storeFcmTokenInBackend(fcmToken ?: "", userID = googleUserId)
                }
            }
        }
    }

    private fun get_downloadurl(
        selectedFormat: String,
        googleidToken: String?,
        googleUserIdd: String?
    ) {
        val url =
            "$BASE_URL/api/journal/file?userID=$googleUserIdd&format=$selectedFormat"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $googleidToken")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failed to retrieve file URL!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "{}")
                    val downloadURL = jsonResponse.optString("downloadURL", "")

                    if (downloadURL.isNotEmpty()) {
                        runOnUiThread {
                            copyToClipboard(downloadURL)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Failed to get file URL!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Error: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun copyToClipboard(downloadURL: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File URL", downloadURL)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "File URL copied to clipboard!", Toast.LENGTH_LONG).show()
    }

    private fun setMonthView() {
        monthYearText.text = monthYearFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)

        val calendarAdapter = CalendarAdapter(this, daysInMonth, selectedDate, journalentries, this)
        val layoutManager = GridLayoutManager(applicationContext, 7)
        calendarRecyclerView.layoutManager = layoutManager
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun daysInMonthArray(date: LocalDate): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth = YearMonth.from(date)

        val daysInMonth = yearMonth.lengthOfMonth()
        val firstofMonth = selectedDate.withDayOfMonth(1)

        val dayOfWeek = firstofMonth.dayOfWeek.value

        for (i in 1..42) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
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
            val googleidtoken = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                .getString("GoogleIDtoken", null)
            val googleuserId = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                .getString("GoogleUserID", null)

            // Allow only past or current dates
            if (!selectedJournalDate.isAfter(LocalDate.now())) {
                val intent = Intent(this, Journal_entries::class.java)
                intent.putExtra("SELECTED_DATE", selectedJournalDate.toString())
                intent.putExtra("GOOGLE_ID", googleuserId)
                intent.putExtra("GOOGLE_TOKEN", googleidtoken)
                if (googleuserId != null && journalentries.contains(selectedJournalDate.toString()) && googleidtoken != null) {
                    fetchJournalEntry(
                        googleuserId,
                        googleidtoken,
                        selectedJournalDate.toString(),
                        intent
                    )
                } else {
                    Log.e(
                        "MainActivity",
                        "User ID is null or the array doesn't have the date, cannot fetch journal entry"
                    )
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Cannot add a journal for future dates!", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun storeFcmTokenInBackend(fcmToken: String, userID: String) {
        Log.d(TAG, "storing fcm token for $userID")
//        val userID = "12345" // Get this dynamically (e.g., after user login)
        val url =
            "$BASE_URL/api/profile/fcmtoken"
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

    private fun logOut() {
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

    private fun fetchJournalEntry(
        userId: String,
        googleidToken: String,
        date: String,
        intent: Intent
    ) {
        val url =
            "$BASE_URL/api/journal/?date=$date&userID=$userId"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $googleidToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    if (responseBody.isNotEmpty()) {
                        intent.putExtra("Journal_Entry_fetched", responseBody)
                    } else {
                        Log.d("Journal Fetch", "No entry found for this date")
                    }
                } else {
                    Log.e("Journal Fetch", "Failed to fetch journal entry: ${response.code}")
                }
                startActivity(intent)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Journal Fetch", "Error fetching journal entry", e)
//                startActivity(intent)
            }
        })
    }

    private fun saveJournalEntries() {
        val sharedPreferences = getSharedPreferences("JournalPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Convert journalentries set to a comma-separated string
        val journalEntriesString = journalentries.joinToString(",")

        // Save it in SharedPreferences
        editor.putString("journal_entries", journalEntriesString)
        editor.apply()
    }

    private fun loadJournalEntries() {
        val sharedPreferences = getSharedPreferences("JournalPrefs", MODE_PRIVATE)

        // Get the saved string, or an empty string if no data is found
        val journalEntriesString = sharedPreferences.getString("journal_entries", "")

        // If the string is not empty, convert it back to a set
        if (!journalEntriesString.isNullOrEmpty()) {
            val dates = journalEntriesString.split(",")  // Split by comma
            journalentries.clear()  // Clear the current set
            journalentries.addAll(dates)  // Add the loaded entries to the set
        }
    }

    private fun isIdTokenExpired(idToken: String): Boolean {
        val parts = idToken.split(".")
        if (parts.size != 3) return true  // Invalid token

        try {
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payload)
            val exp = jsonObject.getLong("exp")  // Expiration time in seconds

            // Convert to milliseconds and compare with current time
            return exp * 1000 < System.currentTimeMillis()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to parse ID token", e)
            return true
        }
    }

    private fun setupButtonListeners(googleidToken: String?, googleUserId: String) {
        findViewById<Button>(R.id.Last_month_button).setOnClickListener {
            selectedDate = selectedDate.minusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.Next_month_button).setOnClickListener {
            selectedDate = selectedDate.plusMonths(1)
            setMonthView()
        }

        findViewById<Button>(R.id.profile_button).setOnClickListener {
            startActivity(Intent(this, ProfileManagement::class.java))
        }

        findViewById<Button>(R.id.log_out_button).setOnClickListener {
            logOut()
        }

        findViewById<Button>(R.id.analytics_button).setOnClickListener {
            Intent(this, AnalyticsActivity::class.java).apply {
                putExtra("THE_DATE", selectedDate.toString())
                putExtra("USER_ID", getSharedPreferences("AppPreferences", MODE_PRIVATE)
                    .getString("GoogleUserID", null))
                startActivity(this)
            }
        }

        findViewById<Button>(R.id.export_button).setOnClickListener {
            val formats = arrayOf("PDF", "CSV")

            AlertDialog.Builder(this)
                .setTitle("Choose file format")
                .setItems(formats) { _, which ->
                    val selectedFormat = formats[which].lowercase()
                    get_downloadurl(selectedFormat, googleidToken, googleUserId)
                }
                .show()
        }
    }

}
