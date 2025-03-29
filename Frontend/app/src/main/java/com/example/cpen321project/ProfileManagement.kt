package com.example.cpen321project

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

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
    private val selectedDays = mutableListOf<Int>()
    private lateinit var notificationManager: NotificationManager
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var customerConfig: PaymentSheet.CustomerConfiguration
    private lateinit var paymentIntentClientSecret: String
    private val profileApiClient = ProfileApiClient()

    companion object {
        private const val TAG = "Profile Management"
        private const val CHANNEL_ID = "channel_id"
        private val permissionsArr = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_management)

        setupViews()
        NotificationHelper.setupNotificationChannel(this)
        notificationManager = NotificationHelper.getNotificationManager(this)
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedDays = prefs.getStringSet("SelectedDays", emptySet()) ?: emptySet()
        selectedDays.clear()
        selectedDays.addAll(savedDays.map { it.toInt() })

        loadUserProfile()
        setupPaymentSheet()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViews() {
        // Back button
        findViewById<Button>(R.id.profile_back_button).setOnClickListener { finish() }

        // Activity list setup
        activityListView = findViewById(R.id.profile_activity_list)
        addActivityButton = findViewById(R.id.profile_add_activity_button)
        activitiesAdapter = ActivitiesAdapter(this, activitiesList)
        activityListView.adapter = activitiesAdapter

        // Button click listeners
        addActivityButton.setOnClickListener { showAddActivityDialog() }
        activityListView.setOnItemLongClickListener { _, _, position, _ ->
            val options = arrayOf("Delete")
            AlertDialog.Builder(this)
                .setTitle("Choose an option")
                .setItems(options) { _, which ->
                    if (which == 0) activitiesList.removeAt(position)
                    activitiesAdapter.notifyDataSetChanged()
                    updateListViewHeight()
                }
                .show()
            true
        }
        findViewById<Button>(R.id.profile_upgrade_button).setOnClickListener { paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "My merchant name",
                customer = customerConfig,
                allowsDelayedPaymentMethods = true
            )
        ) }

        // Save settings button
        val saveSettingsButton: Button = findViewById(R.id.save_settings_button)
        val timePicker: TimePicker = findViewById(R.id.profile_reminder_timepicker)
        val preferredNameText = findViewById<EditText>(R.id.profile_name_input)

        saveSettingsButton.setOnClickListener {
            saveSettings(
                selectedDays,
                timePicker,
                preferredNameText.text.toString().trim()
            )
        }

        // Day selection setup
        setupDaySelection()
    }

    private fun setupPaymentSheet() {
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)
        val jsonBody = JSONObject().apply {
            put("userID", userID)
        }

        profileApiClient.getPaymentSheet(userID) { paymentSheetData ->
            paymentSheetData?.let {
                paymentIntentClientSecret = it.paymentIntent
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = it.customerId,
                    ephemeralKeySecret = it.ephemeralKey
                )
                PaymentConfiguration.init(this, it.publishableKey)
            }
        }
    }

    private fun saveSettings(weekdays: List<Int>, timePicker: TimePicker, preferredName: String) {
        val hour = if (Build.VERSION.SDK_INT >= 23) timePicker.hour else timePicker.currentHour
        val minute = if (Build.VERSION.SDK_INT >= 23) timePicker.minute else timePicker.currentMinute
        val formattedTime = String.format("%02d:%02d", hour, minute)

        // Save locally
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        prefs.edit().putStringSet("SelectedDays", weekdays.map { it.toString() }.toSet()).apply()

        // Send to server
        val userID = prefs.getString("GoogleUserID", null)
        val googleToken = prefs.getString("GoogleIDtoken", null)

        profileApiClient.updateReminder(userID, weekdays, formattedTime, object : ProfileApiClient.ProfileCallback {
            override fun onSuccess(response: String) {
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, response, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(error: String) {
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, error, Toast.LENGTH_LONG).show()
                }
            }
        })

        profileApiClient.updateProfile(userID, googleToken, preferredName, activitiesList,
            object : ProfileApiClient.ProfileCallback {
                override fun onSuccess(response: String) {
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, response, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, error, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }

    private fun loadUserProfile() {
        val userID = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            .getString("GoogleUserID", null)

        profileApiClient.getProfile(userID, object : ProfileApiClient.ProfileCallback {
            override fun onSuccess(response: String) {
                try {
                    val jsonResponse = JSONObject(response)
                    runOnUiThread {
                        // Update preferred name
                        findViewById<EditText>(R.id.profile_name_input).setText(jsonResponse.optString("preferred_name", ""))

                        // Update account status
                        val accountStatus = jsonResponse.optBoolean("isPaid", false)
                        if (accountStatus) {
                            findViewById<TextView>(R.id.profile_account_status).text = "Account Status: Premium"
                            findViewById<Button>(R.id.profile_upgrade_button).visibility = View.GONE
                            findViewById<ImageView>(R.id.stars_icon).visibility = View.VISIBLE
                        } else {
                            findViewById<ImageView>(R.id.stars_icon).visibility = View.GONE
                            findViewById<TextView>(R.id.profile_account_status).text = "Account Status: Free"
                        }

                        // Update activities
                        val activitiesTracking = jsonResponse.optJSONArray("activities_tracking") ?: JSONArray()
                        updateActivitiesList(activitiesTracking)

                        // Update reminder
                        val userReminderTime = jsonResponse.optJSONObject("userReminderTime") ?: JSONObject()
                        updateReminderUI(userReminderTime)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error parsing profile data", e)
                    runOnUiThread {
                        Toast.makeText(this@ProfileManagement, "Error loading profile", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(error: String) {
                runOnUiThread {
                    Toast.makeText(this@ProfileManagement, error, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun updateActivitiesList(activitiesTracking: JSONArray) {
        activitiesList.clear()
        for (i in 0 until activitiesTracking.length()) {
            val activityJson = activitiesTracking.getJSONObject(i)
            activitiesList.add(
                Activity(
                    activityJson.optString("name", ""),
                    activityJson.optDouble("averageValue", 0.0).toFloat(),
                    activityJson.optString("unit", "")
                )
            )
        }
        activitiesAdapter.notifyDataSetChanged()
        updateListViewHeight()
    }

    private fun updateReminderUI(userReminderTime: JSONObject) {
        val weekdays = userReminderTime.optJSONArray("Weekday") ?: JSONArray()
        selectedDays.clear()
        for (i in 0 until weekdays.length()) {
            selectedDays.add(weekdays.getInt(i))
        }
        val daysOfWeek = listOf(
            findViewById<ImageView>(R.id.day_mon),
            findViewById<ImageView>(R.id.day_tue),
            findViewById<ImageView>(R.id.day_wed),
            findViewById<ImageView>(R.id.day_thu),
            findViewById<ImageView>(R.id.day_fri),
            findViewById<ImageView>(R.id.day_sat),
            findViewById<ImageView>(R.id.day_sun)
        )

        for ((index, day) in daysOfWeek.withIndex()) {
            val dayNumber = index + 1
            day.setBackgroundResource(
                if (selectedDays.contains(dayNumber)) R.drawable.circle_purple
                else R.drawable.circle_grey
            )
        }

        val reminderTime = userReminderTime.optString("time", "")
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

    private fun setupDaySelection() {
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
            val dayNumber = index + 1
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

    // Activity List Management
    private fun showAddActivityDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Activity")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val nameInput = EditText(this).apply { hint = "Enter activity name" }
        val valueInput = EditText(this).apply {
            hint = "Enter average value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val unitSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@ProfileManagement,
                android.R.layout.simple_spinner_item,
                arrayOf("Hours", "Minutes", "Times")).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        layout.addView(nameInput)
        layout.addView(valueInput)
        layout.addView(unitSpinner)
        builder.setView(layout)

        builder.setPositiveButton("Add") { _, _ ->
            val activityName = nameInput.text.toString().trim()
            val averageValue = valueInput.text.toString().toFloatOrNull() ?: 0f
            val unit = unitSpinner.selectedItem.toString()

            if (activityName.isNotEmpty() && averageValue > 0) {
                activitiesList.add(Activity(activityName, averageValue, unit))
                activitiesAdapter.notifyDataSetChanged()
                updateListViewHeight()
            } else {
                Toast.makeText(this, "Please enter valid inputs!", Toast.LENGTH_LONG).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }


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

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Canceled -> Log.d(TAG, "Canceled")
            is PaymentSheetResult.Failed -> Log.d(TAG, "Error: ${paymentSheetResult.error}")
            is PaymentSheetResult.Completed -> {
                Log.d(TAG, "Completed")
                runOnUiThread {
                    findViewById<Button>(R.id.profile_upgrade_button).visibility = View.GONE
                    findViewById<TextView>(R.id.profile_account_status).text = "Account Status: Premium"
                    findViewById<ImageView>(R.id.stars_icon).visibility = View.VISIBLE
                }
            }
        }
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



