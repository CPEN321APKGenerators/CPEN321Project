package com.example.cpen321project

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject
import android.widget.Button
import com.android.volley.RequestQueue
import com.stripe.android.paymentsheet.PaymentSheet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult

const val CHANNEL_ID = "channel_id"

class ProfileManagement : AppCompatActivity() {

    private lateinit var activityListView: ListView
    private lateinit var addActivityButton: Button
    private lateinit var activitiesAdapter: ArrayAdapter<String>
    private val activitiesList = mutableListOf<String>()  // List of activities
    private lateinit var reminderSpinner: Spinner
    val permissionsArr = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    lateinit var notificationManager: NotificationManager
    private val selectedDays = mutableListOf<Int>()

    lateinit var paymentSheet: PaymentSheet
    lateinit var customerConfig: PaymentSheet.CustomerConfiguration
    lateinit var paymentIntentClientSecret: String

    companion object {
        private const val TAG = "Profile Management"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getUserProfile()
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_management)
        val saveSettingsButton: Button = findViewById(R.id.save_settings_button)
        val timePicker: TimePicker = findViewById(R.id.profile_reminder_timepicker)
        val preferredNameText = findViewById<EditText>(R.id.profile_name_input)
        val backbutton = findViewById<Button>(R.id.profile_back_button)
        backbutton.setOnClickListener {
            finish()
        }
        // Load the selected days from SharedPreferences
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedDays = prefs.getStringSet("SelectedDays", emptySet()) ?: emptySet()
        selectedDays.clear()
        selectedDays.addAll(savedDays.map { it.toInt() })


        saveSettingsButton.setOnClickListener {
            // Get selected time from TimePicker
            val hour = if (Build.VERSION.SDK_INT >= 23) timePicker.hour else timePicker.currentHour
            val minute = if (Build.VERSION.SDK_INT >= 23) timePicker.minute else timePicker.currentMinute
            val formattedTime = String.format("%02d:%02d", hour, minute)
            val preferredName = preferredNameText.text.toString().trim()

            // Save the selected days locally
            val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            prefs.edit().putStringSet("SelectedDays", selectedDays.map { it.toString() }.toSet()).apply()

            sendReminderSettings(selectedDays, formattedTime)
            sendUserProfile(preferredName, activitiesList)
        }

        if (checkSelfPermission(permissionsArr[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissionsArr, 200)
        }
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "General Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Apply window insets for proper layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        activityListView = findViewById(R.id.profile_activity_list)
        addActivityButton = findViewById(R.id.profile_add_activity_button)


        // Set up the ListView adapter
        activitiesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, activitiesList)
        activityListView.adapter = activitiesAdapter

        // Handle "Add Activity" button click
        addActivityButton.setOnClickListener {
            showAddActivityDialog()
        }

        // Handle long press for edit/delete
        activityListView.setOnItemLongClickListener { _, _, position, _ ->
            showEditDeleteDialog(position)
            true
        }

        findViewById<Button>(R.id.profile_upgrade_button).setOnClickListener() {
            presentPaymentSheet()
        }
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
//        "http://10.0.2.2:3001/payment-sheet".httpPost().responseJson { _, _, result ->
        "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/payment-sheet".httpPost().responseJson { _, _, result ->
            if (result is Result.Success) {
                val responseJson = result.get().obj()
                paymentIntentClientSecret = responseJson.getString("paymentIntent")
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = responseJson.getString("customer"),
                    ephemeralKeySecret = responseJson.getString("ephemeralKey")
                )
                val publishableKey = responseJson.getString("publishableKey")
                PaymentConfiguration.init(this, publishableKey)
            }
        }

        // Ensure ListView expands correctly inside ScrollView
        updateListViewHeight()
        setupDayCircles()
        highlightSelectedDays()

    }

    // Function to show input dialog for adding an activity
    private fun showAddActivityDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Activity")

        // Create an EditText inside the dialog
        val input = EditText(this)
        input.hint = "Enter activity name"
        builder.setView(input)

        // Add "Add" and "Cancel" buttons
        builder.setPositiveButton("Add") { _, _ ->
            val activityName = input.text.toString().trim()
            if (activityName.isNotEmpty()) {
                addNewActivity(activityName)
            } else {
                Toast.makeText(this, "Activity name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        // Show the dialog
        builder.show()
    }

    // Function to show edit/delete options on long press
    private fun showEditDeleteDialog(position: Int) {
        val options = arrayOf("Edit", "Delete")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose an option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> showEditActivityDialog(position)  // Edit selected
                1 -> deleteActivity(position)         // Delete selected
            }
        }
        builder.show()
    }

    // Function to show an edit dialog
    private fun showEditActivityDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Activity")

        // Create an EditText with the current activity name
        val input = EditText(this)
        input.setText(activitiesList[position])
        builder.setView(input)

        // Update activity name on "Save"
        builder.setPositiveButton("Save") { _, _ ->
            val updatedName = input.text.toString().trim()
            if (updatedName.isNotEmpty()) {
                activitiesList[position] = updatedName
                activitiesAdapter.notifyDataSetChanged()
                updateListViewHeight()
            } else {
                Toast.makeText(this, "Activity name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        // Show the dialog
        builder.show()
    }

    // Function to delete an activity
    private fun deleteActivity(position: Int) {
        activitiesList.removeAt(position)
        activitiesAdapter.notifyDataSetChanged()
        updateListViewHeight()
    }

    // Function to add a new activity to the list
    private fun addNewActivity(activityName: String) {
        activitiesList.add(activityName)
        activitiesAdapter.notifyDataSetChanged()
        updateListViewHeight()
    }

    // Function to dynamically adjust ListView height
    private fun updateListViewHeight() {
        val listAdapter = activityListView.adapter ?: return
        var totalHeight = 0
        for (i in 0 until listAdapter.count) {
            val listItem = listAdapter.getView(i, null, activityListView)
            listItem.measure(0, 0)
            totalHeight += listItem.measuredHeight
        }

        val params = activityListView.layoutParams
        params.height = totalHeight + (activityListView.dividerHeight * (listAdapter.count - 1))
        activityListView.layoutParams = params
        activityListView.requestLayout()
    }

    private fun sendReminderSettings(weekdays: List<Int>, time: String) {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile/reminder"
//        val url = "http://10.0.2.2:3001/api/profile/reminder"

        // Construct the JSON body with updated structure
        val json = JSONObject()
        val updatedReminder = JSONObject()
        updatedReminder.put("Weekday", JSONArray(weekdays))
        updatedReminder.put("time", time)

        json.put("userID", userID)
        json.put("updated_reminder", updatedReminder)

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
                    Log.d("Reminder", "Reminder settings updated successfully")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Reminder updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Reminder", "Failed to update reminder settings")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Failed to update reminder settings", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Reminder", "Failed to connect to server", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    private fun setupDayCircles() {
        val daysOfWeek = listOf(
            findViewById<ImageView>(R.id.day_mon),  // Monday -> 1
            findViewById<ImageView>(R.id.day_tue),  // Tuesday -> 2
            findViewById<ImageView>(R.id.day_wed),  // Wednesday -> 3
            findViewById<ImageView>(R.id.day_thu),  // Thursday -> 4
            findViewById<ImageView>(R.id.day_fri),  // Friday -> 5
            findViewById<ImageView>(R.id.day_sat),  // Saturday -> 6
            findViewById<ImageView>(R.id.day_sun)   // Sunday -> 7
        )

        for ((index, day) in daysOfWeek.withIndex()) {
            val dayNumber = index + 1  // Matches [1 = Monday, ..., 7 = Sunday]
            day.setOnClickListener {
                if (selectedDays.contains(dayNumber)) {
                    selectedDays.remove(dayNumber)
                    day.setBackgroundResource(R.drawable.circle_grey)
                } else {
                    selectedDays.add(dayNumber)
                    day.setBackgroundResource(R.drawable.circle_purple)
                }
            }
        }
    }


    private fun getUserProfile() {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        Log.d(TAG, "get user profile ${userID}")
        val url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile?userID=$userID"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonResponse = JSONObject(responseBody)

                        // Extract fields from response
                        val preferredName = jsonResponse.optString("preferred_name", "")
                        val accountStatus = jsonResponse.optBoolean("isPaid", false)
                        val activitiesTracking = jsonResponse.optJSONArray("activities_tracking") ?: JSONArray()
                        val userReminderTime = jsonResponse.optJSONObject("userReminderTime") ?: JSONObject()
                        val weekdays = userReminderTime.optJSONArray("Weekday") ?: JSONArray()
                        val reminderTime = userReminderTime.optString("time", "")

                        // Update UI on the main thread
                        runOnUiThread {
                            // Update preferred name
                            findViewById<EditText>(R.id.profile_name_input).setText(preferredName)

                            if (accountStatus == true) {
                                findViewById<TextView>(R.id.profile_account_status).setText("Account Status: Premium")
                                findViewById<Button>(R.id.profile_upgrade_button).visibility = View.GONE
                            } else {
                                findViewById<TextView>(R.id.profile_account_status).setText("Account Status: Free")
                            }

                            // Update activities tracking list
                            activitiesList.clear()
                            for (i in 0 until activitiesTracking.length()) {
                                activitiesList.add(activitiesTracking.getString(i))
                            }
                            activitiesAdapter.notifyDataSetChanged()
                            updateListViewHeight()

                            // Update reminder days selection
                            selectedDays.clear()
                            for (i in 0 until weekdays.length()) {
                                selectedDays.add(weekdays.getInt(i))
                            }
                            highlightSelectedDays()

                            // Update TimePicker with the saved time
                            if (reminderTime.isNotEmpty()) {
                                val (hour, minute) = reminderTime.split(":").map { it.toInt() }
                                val timePicker: TimePicker = findViewById(R.id.profile_reminder_timepicker)
                                if (Build.VERSION.SDK_INT >= 23) {
                                    timePicker.hour = hour
                                    timePicker.minute = minute
                                } else {
                                    timePicker.currentHour = hour
                                    timePicker.currentMinute = minute
                                }
                            }
                        }
                    }
                } else {
                    Log.e("UserProfile", "Failed to get user profile")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Failed to get user profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserProfile", "Failed to connect to server", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun highlightSelectedDays() {
        val daysOfWeek = listOf(
            findViewById<ImageView>(R.id.day_mon),  // Monday -> 1
            findViewById<ImageView>(R.id.day_tue),  // Tuesday -> 2
            findViewById<ImageView>(R.id.day_wed),  // Wednesday -> 3
            findViewById<ImageView>(R.id.day_thu),  // Thursday -> 4
            findViewById<ImageView>(R.id.day_fri),  // Friday -> 5
            findViewById<ImageView>(R.id.day_sat),  // Saturday -> 6
            findViewById<ImageView>(R.id.day_sun)   // Sunday -> 7
        )

        for ((index, day) in daysOfWeek.withIndex()) {
            val dayNumber = index + 1  // Matches [1 = Monday, ..., 7 = Sunday]
            if (selectedDays.contains(dayNumber)) {
                day.setBackgroundResource(R.drawable.circle_purple)  // Highlight selected days
            } else {
                day.setBackgroundResource(R.drawable.circle_grey)  // Unselected days
            }
        }
        Log.d("Highlight", "Selected Days: $selectedDays")
    }

    private fun upgradeUserAfterPaid() {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile/isPaid"
//        val url = "http://10.0.2.2:3001/api/profile/isPaid"
        // Construct JSON body
        val json = JSONObject()
        json.put("userID", userID)

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
                    Log.d("UserProfile", "User account status updated successfully")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "User account status updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("UserProfile", "Failed to update user account status")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Failed to update user account status", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserProfile", "Failed to connect to server", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    fun presentPaymentSheet() {
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "My merchant name",
                customer = customerConfig,
                // Set `allowsDelayedPaymentMethods` to true if your business handles
                // delayed notification payment methods like US bank accounts.
                allowsDelayedPaymentMethods = true
            )
        )
    }

    fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Log.d(TAG, "Canceled")
            }

            is PaymentSheetResult.Failed -> {
                Log.d(TAG, "Error: ${paymentSheetResult.error}")
            }

            is PaymentSheetResult.Completed -> {
                // Display for example, an order confirmation screen
                Log.d(TAG, "Completed")
                findViewById<Button>(R.id.profile_upgrade_button).visibility = View.GONE
                upgradeUserAfterPaid()
                findViewById<TextView>(R.id.profile_account_status).setText("Account Status: Premium")
            }
        }
    }

    private fun sendUserProfile(preferredName: String, activities: List<String>) {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val url = "http://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile"

        // Construct JSON body
        val json = JSONObject()
        json.put("userID", userID)
        json.put("preferred_name", preferredName)
        json.put("activities_tracking", JSONArray(activities))

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
                    Log.d("UserProfile", "User profile updated successfully")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("UserProfile", "Failed to update user profile")
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("UserProfile", "Failed to connect to server", e)
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

}
