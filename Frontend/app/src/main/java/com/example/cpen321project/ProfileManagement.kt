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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

const val CHANNEL_ID = "channel_id"

// data class to hold activity details
data class Activity(
    var name: String,
    var averageValue: Float,
    var unit: String
)

class ProfileManagement : AppCompatActivity() {

    private lateinit var activityListView: ListView
    private lateinit var addActivityButton: Button
    private lateinit var activitiesAdapter: ArrayAdapter<Activity>
    private val activitiesList = mutableListOf<Activity>()
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
//        activitiesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, activitiesList)
//        activityListView.adapter = activitiesAdapter

        // Set up the custom ListView adapter
        activitiesAdapter = ActivitiesAdapter(this, activitiesList)
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
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val jsonBody = JSONObject()
        jsonBody.put("userID", userID)

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

//        "http://10.0.2.2:3001/api/payment-sheet".httpPost()
        "https://cpen321project-journal.duckdns.org/api/payment-sheet".httpPost()
            .header("Content-Type" to "application/json")
            .body(jsonBody.toString())
            .responseJson { _, _, result ->
                if (result is Result.Success) {
                    val responseJson = result.get().obj()
                    Log.d(TAG, "Response from payment sheet: ${responseJson}")
                    paymentIntentClientSecret = responseJson.getString("paymentIntent")
                    customerConfig = PaymentSheet.CustomerConfiguration(
                        id = responseJson.getString("customer"),
                        ephemeralKeySecret = responseJson.getString("ephemeralKey")
                    )
                    val publishableKey = responseJson.getString("publishableKey")
                    PaymentConfiguration.init(this, publishableKey)
                } else {
                    Log.e(TAG, "Failed to get payment sheet: ${result}")
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

        // Create a LinearLayout to hold the inputs
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Activity Name Input
        val nameInput = EditText(this)
        nameInput.hint = "Enter activity name"
        layout.addView(nameInput)

        // Average Value Input
        val valueInput = EditText(this)
        valueInput.hint = "Enter average value"
        valueInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        layout.addView(valueInput)

        // Unit Dropdown
        val units = arrayOf("Hours", "Minutes", "Times")
        val unitSpinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter
        layout.addView(unitSpinner)

        builder.setView(layout)

        // Add "Add" and "Cancel" buttons
        builder.setPositiveButton("Add") { _, _ ->
            val activityName = nameInput.text.toString().trim()
            val averageValue = valueInput.text.toString().toFloatOrNull() ?: 0f
            val unit = unitSpinner.selectedItem.toString()

            if (activityName.isNotEmpty() && averageValue > 0) {
                addNewActivity(activityName, averageValue, unit)
            } else {
                Toast.makeText(this, "Please enter valid inputs!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
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
                1 -> deleteActivity(position)           // Delete selected
            }
        }
        builder.show()
    }

    // Function to show an edit dialog
    private fun showEditActivityDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Activity")

        // Create a LinearLayout to hold the inputs
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // Get the current activity details
        val currentActivity = activitiesList[position]

        // Activity Name Input
        val nameInput = EditText(this)
        nameInput.hint = "Enter activity name"
        nameInput.setText(currentActivity.name)
        layout.addView(nameInput)

        // Average Value Input
        val valueInput = EditText(this)
        valueInput.hint = "Enter average value"
        valueInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        valueInput.setText(currentActivity.averageValue.toString())
        layout.addView(valueInput)

        // Unit Dropdown
        val units = arrayOf("Hours", "Minutes", "Times")
        val unitSpinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = adapter
        unitSpinner.setSelection(units.indexOf(currentActivity.unit))
        layout.addView(unitSpinner)

        builder.setView(layout)

        // Update activity on "Save"
        builder.setPositiveButton("Save") { _, _ ->
            val updatedName = nameInput.text.toString().trim()
            val updatedValue = valueInput.text.toString().toFloatOrNull() ?: 0f
            val updatedUnit = unitSpinner.selectedItem.toString()

            if (updatedName.isNotEmpty() && updatedValue > 0) {
                currentActivity.name = updatedName
                currentActivity.averageValue = updatedValue
                currentActivity.unit = updatedUnit
                activitiesAdapter.notifyDataSetChanged()
                updateListViewHeight()
            } else {
                Toast.makeText(this, "Please enter valid inputs!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


    // Function to delete an activity
    private fun deleteActivity(position: Int) {
        activitiesList.removeAt(position)
        activitiesAdapter.notifyDataSetChanged()
        updateListViewHeight()
    }


    // Function to add a new activity to the list
    private fun addNewActivity(name: String, averageValue: Float, unit: String) {
        val newActivity = Activity(name, averageValue, unit)
        activitiesList.add(newActivity)
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
        val url = "https://cpen321project-journal.duckdns.org/api/profile/reminder"
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
        Log.d(TAG, "get user profile $userID")
        val url = "https://cpen321project-journal.duckdns.org/api/profile?userID=$userID"

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
                        Log.d(TAG, "get user response: ${jsonResponse}")

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

                            // Update account status
                            if (accountStatus) {
                                findViewById<TextView>(R.id.profile_account_status).text = "Account Status: Premium"
                                findViewById<Button>(R.id.profile_upgrade_button).visibility = View.GONE
                            } else {
                                findViewById<TextView>(R.id.profile_account_status).text = "Account Status: Free"
                            }

                            // Update activities tracking list
                            activitiesList.clear()
                            for (i in 0 until activitiesTracking.length()) {
                                val activityJson = activitiesTracking.getJSONObject(i)
                                val activityName = activityJson.optString("name", "")
                                val averageValue = activityJson.optDouble("averageValue", 0.0).toFloat()
                                val unit = activityJson.optString("unit", "")

                                // Create a new Activity object and add it to the list
                                val activity = Activity(activityName, averageValue, unit)
                                activitiesList.add(activity)
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
                findViewById<TextView>(R.id.profile_account_status).setText("Account Status: Premium")
                //postrequest to make the user paid
                User_paid_to_premium()
            }
        }
    }

    private fun User_paid_to_premium() {
        val client = OkHttpClient()
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val json = JSONObject().apply {
            put("userID", userID)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://cpen321project-journal.duckdns.org/api/profile/isPaid")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Payment update failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Payment saved successfully!, You are a premium user",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error: ${response.body?.string()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun sendUserProfile(preferredName: String, activities: List<Activity>) {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val googleidtoken = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleIDtoken", null)
        val url = "https://cpen321project-journal.duckdns.org/api/profile"

        // Construct JSON body
        val json = JSONObject()
        json.put("userID", userID)
        json.put("preferred_name", preferredName)

        val activitiesArray = JSONArray()
        for (activity in activities) {
            val activityJson = JSONObject()
            activityJson.put("name", activity.name)
            activityJson.put("averageValue", activity.averageValue)
            activityJson.put("unit", activity.unit)
            activitiesArray.put(activityJson)
        }
        json.put("activities_tracking", activitiesArray)
        json.put("googleToken", googleidtoken)

        val client = OkHttpClient()
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), json.toString()
        )
        Log.d(TAG, "Request body of send user profile: ${requestBody}")

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

class ActivitiesAdapter(context: Context, private val activities: List<Activity>) :
    ArrayAdapter<Activity>(context, 0, activities) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val activity = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(
            android.R.layout.simple_list_item_2, parent, false
        )
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)

        text1.text = activity?.name
        text2.text = "Average: ${activity?.averageValue} ${activity?.unit}"
        return view
    }
}

