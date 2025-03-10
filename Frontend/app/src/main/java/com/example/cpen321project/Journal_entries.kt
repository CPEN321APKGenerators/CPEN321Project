package com.example.cpen321project

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class Journal_entries : AppCompatActivity() {

    private lateinit var journaldatetext: TextView
    private lateinit var journalentrytext: EditText
    private lateinit var backtocalendar: Button
    private lateinit var editentry: ImageButton
    private lateinit var deleteentry: ImageButton
    private lateinit var add_image: ImageButton
    private lateinit var save_entry: Button
    private lateinit var journalImageview: ImageView
    private var selectedDate: String? = null
    private val REQUEST_CODE_STORAGE_PERMISSION = 101
    private val REQUEST_CODE_CAMERA_PERMISSION = 102
    private var isPaidUser = false
    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var chatInput: EditText
    private lateinit var sendChatButton: Button
    private val client = OkHttpClient()
    private val chatbotUrl =
        "http://ec2-54-234-28-190.compute-1.amazonaws.com:5005/webhooks/rest/webhook"
    private val BASE_URL = "https://cpen321project-journal.duckdns.org"
    private var userID: String? = null
    private var user_google_token: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journal_entries)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.journalview)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        journaldatetext = findViewById(R.id.journalDateText)
        journalentrytext = findViewById(R.id.journalEntryInput)
        backtocalendar = findViewById(R.id.Backbuttonentries)
        editentry = findViewById(R.id.editbutton)
        deleteentry = findViewById(R.id.deletebutton)
        add_image = findViewById(R.id.addimageButton)
        save_entry = findViewById(R.id.Saveentrybutton)
        journalImageview = findViewById(R.id.journalImageView)
        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        chatInput = findViewById(R.id.chatInput)
        sendChatButton = findViewById(R.id.sendChatButton)

        selectedDate = intent.getStringExtra("SELECTED_DATE") ?: ""
        journaldatetext.text = "Journal Entry for $selectedDate"
        userID = intent.getStringExtra("GOOGLE_ID")
        user_google_token = intent.getStringExtra("GOOGLE_TOKEN")

        val entrytext = intent.getStringExtra("Journal_Entry_fetched") ?: ""
        if (entrytext.isNotEmpty()) {
            val json = JSONObject(entrytext)
            val journalObject = json.getJSONObject("journal")
            val text = journalObject.getString("text")
            journalentrytext.setText(text)
            val mediaArray = journalObject.getJSONArray("media")
            if (mediaArray.length() > 0) {
                val base64Image =
                    mediaArray.getString(0)  // Get the first image (adjust if multiple)
                val bitmap = decodeBase64ToBitmap(base64Image)
                journalImageview.setImageBitmap(bitmap) // Set the image in ImageView
                journalImageview.visibility = View.VISIBLE
            }
            // Show journal UI
            journalentrytext.visibility = View.VISIBLE
            save_entry.visibility = View.VISIBLE

            // Hide chatbot UI
            chatScrollView.visibility = View.GONE
            chatInput.visibility = View.GONE
            sendChatButton.visibility = View.GONE
        } else {
            // Hide journal UI
            journalentrytext.visibility = View.GONE
            save_entry.visibility = View.GONE
            journalImageview.visibility = View.GONE

            // Show chatbot UI
            chatScrollView.visibility = View.VISIBLE
            chatInput.visibility = View.VISIBLE
            sendChatButton.visibility = View.VISIBLE
            save_entry.visibility = View.VISIBLE
        }

        val journalexisted = intent.getBooleanExtra("Pre_existing journal", false)

        journalentrytext.isEnabled = false

        backtocalendar.setOnClickListener() {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            fetchJournalEntry(intent)
        }

        editentry.setOnClickListener() {
            if (journalentrytext.visibility == View.VISIBLE) {
                journalentrytext.isEnabled = true
                journalentrytext.requestFocus()
            } else {
                Toast.makeText(this, "Write a Journal to begin editing", Toast.LENGTH_SHORT).show()
            }
        }

        deleteentry.setOnClickListener() {
            showdeleteconformationpopup()
        }

        save_entry.setOnClickListener() {
            if (!journalexisted) {
                saveentry()
            } else {
                updateJournalEntry()
            }
        }

        Userpaid { isPaid ->
            isPaidUser = isPaid
            if (isPaidUser) {
                Log.d("User Status", "User is a paid user")
            } else {
                Log.d("User Status", "User is NOT a paid user")
            }
        }

        add_image.setOnClickListener() {
            if (isPaidUser) {
                showUploadOptions()
            } else {
                Toast.makeText(this, "Upgrade to upload media!", Toast.LENGTH_SHORT).show()
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

        journalImageview.setOnClickListener() {
            showdeletiondialog()
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

    private fun Userpaid(callback: (Boolean) -> Unit) {

        val request = Request.Builder()
            .url("$BASE_URL/api/profile/isPaid/?userID=$userID")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        val jsonObject = JSONObject(responseBody) // Parse JSON
                        val paidUser = jsonObject.optBoolean("isPaid", false) // Extract "isPaid"
                        callback(paidUser) // Send result to callback
                    } catch (e: JSONException) {
                        Log.e("User Paid Fetch", "Failed to parse JSON response", e)
                        callback(false) // Assume false if parsing fails
                    }
                } else {
                    Log.e("User Paid Fetch", "Failed to fetch user status: ${response.code}")
                    callback(false) // Assume false if request fails
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("User Paid Fetch", "Error fetching user status", e)
                callback(false) // Assume false on failure
            }
        })
    }


    private fun sendMessageToChatbot(message: String) {
        val json = JSONObject()
//        if(journal_flag) {
        json.apply {
            put("date", selectedDate)  // Must be in ISO8601 format (yyyy-MM-dd)
            put("userID", userID)
            put("google_token", user_google_token)
            put("message", message)
        }
//        } else {
//            json.apply {
//                put("message", message)
//            }
//        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(chatbotUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failed to connect to chatbot",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val responseArray = JSONArray(responseData)  // Parse response as JSONArray
                    val botMessages = StringBuilder()

                    for (i in 0 until responseArray.length()) {
                        val messageObject = responseArray.getJSONObject(i)  // Get each JSON object
                        val botMessage = messageObject.getString("text")  // Extract text
                        botMessages.append(botMessage).append("\n")  // Append to a string

//                        if (messageObject.has("Journalentry")) {
//                            journal_flag = messageObject.getBoolean("Journalentry")
//                        }
                    }

                    runOnUiThread {
                        addChatMessage("Bot: ${botMessages.toString().trim()}", false)
                    }
                }
            }
        })
    }

    private fun addChatMessage(text: String, isUser: Boolean) {
        val messageView = TextView(this)
        messageView.text = text
        messageView.textSize = 16f
        messageView.setPadding(10, 10, 10, 10)
        messageView.setTextColor(Color.WHITE)  // White text for better contrast
        messageView.setTypeface(null, Typeface.BOLD)

        if (isUser) {
            messageView.background = ContextCompat.getDrawable(this, R.drawable.chat_bubble_user)
        } else {
            messageView.background = ContextCompat.getDrawable(this, R.drawable.chat_bubble_bot)
        }

        chatContainer.addView(messageView)

        // Scroll to the latest message
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

        val mediaArray = JSONArray()
        val base64Image = convertImageViewToBase64(journalImageview)
        if (base64Image != null) {
            mediaArray.put(base64Image)  // Add Base64 image string to the JSON array
        }

        val json =
            try {
                JSONObject().apply {
                    put("date", selectedDate)  // Must be in ISO8601 format (yyyy-MM-dd)
                    put("userID", userID)
                    put("text", journalText)
                    put("media", mediaArray)  // Ensure this doesn't exceed size limits
                }
            } catch (e: JSONException) {
                Log.e("JSON Error", "Failed to create JSON object", e)
                null  // Return null or handle error accordingly
            }

        if (json != null) {
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/journal")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $user_google_token")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            "Failed to save journal!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(
                                applicationContext,
                                "Journal saved successfully!",
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
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra("added_date", selectedDate.toString())
            startActivity(intent)
            finish()
        } else {
            Log.e("Request Error", "JSON object is null, request not sent")
        }
    }

    private fun convertImageViewToBase64(imageView: ImageView): String? {
        val drawable = imageView.drawable ?: return null
        val bitmap = (drawable as BitmapDrawable).bitmap

        // Resize image to prevent large Base64 strings
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(
            Bitmap.CompressFormat.JPEG,
            50,
            outputStream
        )  // Compress as JPEG with 50% quality
        val byteArray = outputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.NO_WRAP) // Convert to Base64
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }

    private fun showdeleteconformationpopup() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Delete Journal Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Yes") { _, _ ->
                deleteJournalEntry()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dismiss dialog, do nothing
            }
            .create()

        alertDialog.show()
    }

    private fun updateJournalEntry() {
        val updatedText = journalentrytext.text.toString().trim()
        if ((updatedText.isEmpty() && journalImageview.drawable == null) || selectedDate == null) {
            Toast.makeText(this, "Journal entry cannot be empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaArray = JSONArray()
        val base64Image = convertImageViewToBase64(journalImageview)
        if (base64Image != null) {
            mediaArray.put(base64Image)  // Add Base64 image string to the JSON array
        }

        val json = JSONObject().apply {
            put("date", selectedDate)
            put("userID", userID) // Replace with actual user ID
            put("text", updatedText)
            put("media", mediaArray)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/api/journal")
            .put(requestBody)
            .addHeader("Authorization", "Bearer $user_google_token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failed to update journal!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Journal updated!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error updating journal!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("added_date", selectedDate.toString())
        startActivity(intent)
        finish()
    }

    private fun fetchJournalEntry(intent: Intent) {

        val request = Request.Builder()
            .url("$BASE_URL/api/journal?date=$selectedDate&userID=$userID")
            .get()
            .addHeader("Authorization", "Bearer $user_google_token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    if (!responseBody.isNullOrEmpty()) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val journalObject = jsonResponse.getJSONObject("journal")
                            val text = journalObject.getString("text")
                            val mediaArray = journalObject.getJSONArray("media")

                            // Check if there is any text or media saved
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
                        }
                    } else {
                        Log.d("Journal Entry", "Empty response from server")
                    }
                } else {
                    Log.e("Journal Fetch", "Failed to fetch journal entry: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("Journal Fetch", "Error fetching journal entry", e)
            }
        })
    }

    private fun deleteJournalEntry() {
        val request = Request.Builder()
            .url("$BASE_URL/api/journal?date=$selectedDate&userID=$userID")  // Replace with actual user ID
            .delete()
            .addHeader("Authorization", "Bearer $user_google_token")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        "Failed to delete journal!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Journal deleted!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Error deleting journal!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
        journalentrytext.setText("")
        // Close activity and go back to main screen
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.putExtra("deleted_date", selectedDate.toString())
        startActivity(intent)
        finish()
    }

    private fun showUploadOptions() {
        val options = arrayOf("Select from Gallery", "Take a Photo")

        AlertDialog.Builder(this)
            .setTitle("Upload Media")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestStoragePermission()  // Select from Gallery
                    1 -> requestCameraPermission()   // Take a Photo
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {

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
            != PackageManager.PERMISSION_GRANTED
        ) {

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
        pickImageLauncher.launch("image/*") // Opens gallery for image selection
    }

    private fun openCamera() {
        if (!isPaidUser) {
            Toast.makeText(this, "Upgrade to upload media!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoLauncher.launch(intent) // Opens camera
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