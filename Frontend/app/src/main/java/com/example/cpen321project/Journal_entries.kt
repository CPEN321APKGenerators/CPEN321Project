package com.example.cpen321project

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONException
import org.json.JSONObject

class Journal_entries : AppCompatActivity() {
    private lateinit var journalentrytext: EditText
    private lateinit var save_entry: Button
    private lateinit var journalImageview: ImageView
    private var selectedDate: String? = null
    private val REQUEST_CODE_STORAGE_PERMISSION = 101
    private val REQUEST_CODE_CAMERA_PERMISSION = 102
    private var isPaidUser = false
    private lateinit var chatScrollView: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var sendChatButton: Button
    private var userID: String? = null
    private var user_google_token: String? = null
    private lateinit var journalApiClient: JournalApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journal_entries)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.journalview)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        journalApiClient = JournalApiClient()
        initializeViews()
        setupListeners()
        checkUserPaidStatus()
    }

    private fun initializeViews() {
        journalentrytext = findViewById(R.id.journalEntryInput)
        journalImageview = findViewById(R.id.journalImageView)
        chatScrollView = findViewById(R.id.chatScrollView)
        chatInput = findViewById(R.id.chatInput)
        sendChatButton = findViewById(R.id.sendChatButton)
        save_entry = findViewById(R.id.Saveentrybutton)

        selectedDate = intent.getStringExtra("SELECTED_DATE") ?: ""
        findViewById<TextView>(R.id.journalDateText).text = "Journal Entry for $selectedDate"
        userID = intent.getStringExtra("GOOGLE_ID")
        user_google_token = intent.getStringExtra("GOOGLE_TOKEN")

        val entrytext = intent.getStringExtra("Journal_Entry_fetched") ?: ""
        loadscreenifexists(entrytext)
        journalentrytext.isEnabled = false
    }

    private fun setupListeners() {
        findViewById<Button>(R.id.Backbuttonentries).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            fetchJournalEntry(intent)
        }

        findViewById<ImageButton>(R.id.editbutton).setOnClickListener {
            if (journalentrytext.visibility == View.VISIBLE) {
                journalentrytext.isEnabled = true
                journalentrytext.requestFocus()
            } else {
                Toast.makeText(this, "Write a Journal to begin editing", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.deletebutton).setOnClickListener {
            showdeleteconformationpopup()
        }

        save_entry.setOnClickListener {
            if (journalentrytext.text.toString().trim().isEmpty()) {
                saveentry()
            } else {
                updateJournalEntry()
            }
        }

        findViewById<ImageButton>(R.id.addimageButton).setOnClickListener {
            if (isPaidUser) {
                showUploadOptions()
            } else {
                Toast.makeText(this, "Upgrade to upload media!", Toast.LENGTH_LONG).show()
            }
        }

        sendChatButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                addChatMessage("You: $message", true)
                sendMessageToChatbot(message)
                chatInput.text.clear()
            }
        }

        journalImageview.setOnClickListener {
            showdeletiondialog()
        }
    }

    private fun checkUserPaidStatus() {
        journalApiClient.checkUserPaidStatus(userID) { isPaid ->
            isPaidUser = isPaid
            if (isPaidUser) {
                Log.d("User Status", "User is a paid user")
            } else {
                Log.d("User Status", "User is NOT a paid user")
            }
        }
    }

    private fun loadscreenifexists(Savedentry: String) {
        if (Savedentry.isNotEmpty()) {
            val json = JSONObject(Savedentry)
            val journalObject = json.getJSONObject("journal")
            val text = journalObject.getString("text")
            journalentrytext.setText(text)
            val mediaArray = journalObject.getJSONArray("media")
            if (mediaArray.length() > 0) {
                val base64Image = mediaArray.getString(0)
                val bitmap = journalApiClient.decodeBase64ToBitmap(base64Image)
                journalImageview.setImageBitmap(bitmap)
                journalImageview.visibility = View.VISIBLE
            }

            journalentrytext.visibility = View.VISIBLE
            save_entry.visibility = View.VISIBLE
            chatScrollView.visibility = View.GONE
            chatInput.visibility = View.GONE
            sendChatButton.visibility = View.GONE
        } else {
            journalentrytext.visibility = View.GONE
            save_entry.visibility = View.GONE
            journalImageview.visibility = View.GONE
            chatScrollView.visibility = View.VISIBLE
            chatInput.visibility = View.VISIBLE
            sendChatButton.visibility = View.VISIBLE
            save_entry.visibility = View.VISIBLE
        }
    }

    private fun showdeletiondialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Delete") { _, _ ->
                deleteImageFromJournal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteImageFromJournal() {
        journalImageview.setImageDrawable(null)
    }

    private fun sendMessageToChatbot(message: String) {
        journalApiClient.sendChatMessage(userID, user_google_token, selectedDate, message,
            object : JournalApiClient.JournalCallback {
                override fun onSuccess(response: String) {
                    try {
                        val responseObject = JSONObject(response)
                        val responseArray = responseObject.getJSONArray("messages")
                        val botMessages = StringBuilder()

                        for (i in 0 until responseArray.length()) {
                            val messageObject = responseArray.getJSONObject(i)
                            val botMessage = messageObject.getString("text")
                            botMessages.append(botMessage).append("\n")
                        }

                        runOnUiThread {
                            addChatMessage("Bot: ${botMessages.toString().trim()}", false)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Error parsing chatbot response",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Failed to connect to chatbot",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun addChatMessage(text: String, isUser: Boolean) {
        val messageView = TextView(this)
        messageView.text = text
        messageView.textSize = 16f
        messageView.setPadding(10, 10, 10, 10)
        messageView.setTextColor(Color.WHITE)
        messageView.setTypeface(null, Typeface.BOLD)

        if (isUser) {
            messageView.background = ContextCompat.getDrawable(this, R.drawable.chat_bubble_user)
        } else {
            messageView.background = ContextCompat.getDrawable(this, R.drawable.chat_bubble_bot)
        }

        findViewById<LinearLayout>(R.id.chatContainer).addView(messageView)

        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun saveentry() {
        val journalText = journalentrytext.text.toString().trim()
        if ((journalText.isEmpty() && journalImageview.drawable == null) || selectedDate == null) {
            Toast.makeText(this, "Journal entry cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        val params = JournalApiClient.JournalEntryParams(selectedDate, userID, user_google_token, journalText, journalImageview)

        journalApiClient.saveJournalEntry(
            params,
            object : JournalApiClient.JournalCallback {
                override fun onSuccess(response: String) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Journal saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@Journal_entries, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.putExtra("added_date", selectedDate.toString())
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun updateJournalEntry() {
        val updatedText = journalentrytext.text.toString().trim()
        if ((updatedText.isEmpty() && journalImageview.drawable == null) || selectedDate == null) {
            Toast.makeText(this, "Journal entry cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }
        val params = JournalApiClient.JournalEntryParams(selectedDate, userID, user_google_token, updatedText, journalImageview)

        journalApiClient.updateJournalEntry(
            params,
            object : JournalApiClient.JournalCallback {
                override fun onSuccess(response: String) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Journal updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@Journal_entries, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.putExtra("added_date", selectedDate.toString())
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun fetchJournalEntry(intent: Intent) {
        journalApiClient.fetchJournalEntry(
            selectedDate,
            userID,
            user_google_token,
            object : JournalApiClient.JournalCallback {
                override fun onSuccess(response: String) {
                    try {
                        val jsonResponse = JSONObject(response)
                        val journalObject = jsonResponse.getJSONObject("journal")
                        val text = journalObject.getString("text")
                        val mediaArray = journalObject.getJSONArray("media")

                        if (text.isNotEmpty() || mediaArray.length() > 0) {
                            runOnUiThread {
                                intent.putExtra("added_date", selectedDate)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Log.d("Journal Entry", "No journal entry found for this date")
                            startActivity(intent)
                            finish()
                        }
                    } catch (e: JSONException) {
                        Log.e("Journal Fetch", "Error parsing JSON", e)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(error: String) {
                    Log.e("Journal Fetch", error)
                    startActivity(intent)
                    finish()
                }
            })
    }

    private fun showdeleteconformationpopup() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Journal Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Yes") { _, _ ->
                deleteJournalEntry()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun deleteJournalEntry() {
        journalApiClient.deleteJournalEntry(
            selectedDate,
            userID,
            user_google_token,
            object : JournalApiClient.JournalCallback {
                override fun onSuccess(response: String) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Journal deleted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        journalentrytext.setText("")
                        val intent = Intent(this@Journal_entries, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        intent.putExtra("deleted_date", selectedDate.toString())
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onFailure(error: String) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun showUploadOptions() {
        val options = arrayOf("Select from Gallery", "Take a Photo")

        AlertDialog.Builder(this)
            .setTitle("Upload Media")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestStoragePermission()
                    1 -> requestCameraPermission()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        } else {
            openMediaPicker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openMediaPicker()
                } else {
                    Toast.makeText(
                        this,
                        "Permission Denied! Cannot access gallery.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            REQUEST_CODE_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera Permission Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openMediaPicker() {
        if (!isPaidUser) {
            Toast.makeText(this, "Upgrade to upload media!", Toast.LENGTH_SHORT).show()
            return
        }
        pickImageLauncher.launch("image/*")
    }

    private fun openCamera() {
        if (!isPaidUser) {
            Toast.makeText(this, "Upgrade to upload media!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(intent)
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            journalImageview.setImageURI(it)
            journalImageview.visibility = View.VISIBLE
            Toast.makeText(this, "Image Selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photo = result.data?.extras?.get("data") as? Bitmap
            photo?.let {
                journalImageview.setImageBitmap(it)
                journalImageview.visibility = View.VISIBLE
                Toast.makeText(this, "Photo Captured!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}