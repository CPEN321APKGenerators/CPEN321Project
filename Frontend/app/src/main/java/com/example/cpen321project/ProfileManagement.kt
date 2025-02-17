package com.example.cpen321project

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileManagement : AppCompatActivity() {

    private lateinit var activityListView: ListView
    private lateinit var addActivityButton: Button
    private lateinit var activitiesAdapter: ArrayAdapter<String>
    private val activitiesList = mutableListOf<String>()  // List of activities
    private lateinit var reminderSpinner: Spinner
    private val reminderOptions = arrayOf("Everyday", "Never", "Every 2 Days", "Once a Week")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_management)

        // Apply window insets for proper layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        activityListView = findViewById(R.id.profile_activity_list)
        addActivityButton = findViewById(R.id.profile_add_activity_button)

        // Initialize Spinner
        reminderSpinner = findViewById(R.id.profile_reminder_dropdown)

        // Create an ArrayAdapter with the options
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, reminderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        reminderSpinner.adapter = adapter

        // Handle item selection
        reminderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedOption = reminderOptions[position]
                Toast.makeText(
                    this@ProfileManagement,
                    "Selected: $selectedOption",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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
}
