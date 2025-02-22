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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_management)
        val saveSettingsButton: Button = findViewById(R.id.save_settings_button)
        val timePicker: TimePicker = findViewById(R.id.profile_reminder_timepicker)
        saveSettingsButton.setOnClickListener {
            // Get selected time from TimePicker
            val hour = if (Build.VERSION.SDK_INT >= 23) timePicker.hour else timePicker.currentHour
            val minute = if (Build.VERSION.SDK_INT >= 23) timePicker.minute else timePicker.currentMinute

            // Convert time to 24-hour format (e.g., "21:00")
            val formattedTime = String.format("%02d:%02d", hour, minute)

            // Send reminder settings with selected days and time
            sendReminderSettings(selectedDays, formattedTime)
        }


        setupDayCircles()

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

        // Ensure ListView expands correctly inside ScrollView
        updateListViewHeight()
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
        val userID = "12345"  // Replace with actual user ID
//        val url = "https://ec2-35-183-201-213.ca-central-1.compute.amazonaws.com/api/profile/reminder"
        val url = "http://10.0.2.2:3001/api/profile/reminder"

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
            val dayNumber = index + 1  // Now it matches [1 = Monday, ..., 7 = Sunday]
            day.setOnClickListener {
                if (selectedDays.contains(dayNumber)) {
                    // Deselect day
                    selectedDays.remove(dayNumber)
                    day.setBackgroundResource(R.drawable.circle_grey)
                } else {
                    // Select day
                    selectedDays.add(dayNumber)
                    day.setBackgroundResource(R.drawable.circle_purple)
                }
            }
        }
    }


}
