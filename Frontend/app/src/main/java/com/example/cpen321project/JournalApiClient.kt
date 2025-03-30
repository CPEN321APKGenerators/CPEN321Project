package com.example.cpen321project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.util.Log
import android.widget.ImageView
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

class JournalApiClient {
    private val client = OkHttpClient()
    private val BASE_URL = "https://cpen321project-journal.duckdns.org"
    private val chatbotUrl = "https://chatbot-wrapper.duckdns.org:3001/api/chat"

    interface JournalCallback {
        fun onSuccess(response: String)
        fun onFailure(error: String)
    }

    data class JournalEntryParams(
        val date: String?,
        val userID: String?,
        val googleToken: String?,
        val text: String,
        val imageView: ImageView
    )

    fun checkUserPaidStatus(userID: String?, callback: (Boolean) -> Unit) {
        val request = Request.Builder()
            .url("$BASE_URL/api/profile/isPaid/?userID=$userID")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val paidUser = jsonObject.optBoolean("isPaid", false)
                        callback(paidUser)
                    } catch (e: JSONException) {
                        Log.e("User Paid Fetch", "Failed to parse JSON response", e)
                        callback(false)
                    }
                } else {
                    Log.e("User Paid Fetch", "Failed to fetch user status: ${response.code}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e("User Paid Fetch", "Error fetching user status", e)
                callback(false)
            }
        })
    }

    fun saveJournalEntry(
        params: JournalEntryParams,
        callback: JournalCallback
    ) {
        val mediaArray = JSONArray()
        val base64Image = convertImageViewToBase64(params.imageView)
        if (base64Image != null) {
            mediaArray.put(base64Image)
        }

        val json = try {
            JSONObject().apply {
                put("date", params.date)
                put("userID", params.userID)
                put("text", params.text)
                put("media", mediaArray)
            }
        } catch (e: JSONException) {
            Log.e("JSON Error", "Failed to create JSON object", e)
            null
        }

        if (json != null) {
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/api/journal")
                .post(requestBody)
                .addHeader("Authorization", "Bearer ${params.googleToken}")
                .build()

            client.newCall(request).enqueue(createJournalCallback(callback, "save"))
        } else {
            callback.onFailure("Failed to create journal data")
        }
    }

    fun updateJournalEntry(
        params: JournalEntryParams,
        callback: JournalCallback
    ) {
        val mediaArray = JSONArray()
        val base64Image = convertImageViewToBase64(params.imageView)
        if (base64Image != null) {
            mediaArray.put(base64Image)
        }

        val json = JSONObject().apply {
            put("date", params.date)
            put("userID", params.userID)
            put("text", params.text)
            put("media", mediaArray)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/api/journal")
            .put(requestBody)
            .addHeader("Authorization", "Bearer ${params.googleToken}")
            .build()

        client.newCall(request).enqueue(createJournalCallback(callback, "update"))
    }

    private fun createJournalCallback(callback: JournalCallback, action: String) = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callback.onFailure("Failed to $action journal!")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                callback.onSuccess("Journal ${action}d successfully!")
            } else {
                callback.onFailure("Error: ${response.body?.string()}")
            }
        }
    }

    fun fetchJournalEntry(
        date: String?,
        userID: String?,
        googleToken: String?,
        callback: JournalCallback
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/api/journal?date=$date&userID=$userID")
            .get()
            .addHeader("Authorization", "Bearer $googleToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrEmpty()) {
                        callback.onSuccess(responseBody)
                    } else {
                        callback.onFailure("Empty response from server")
                    }
                } else {
                    callback.onFailure("Failed to fetch journal entry: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Error fetching journal entry: ${e.message}")
            }
        })
    }

    fun deleteJournalEntry(
        date: String?,
        userID: String?,
        googleToken: String?,
        callback: JournalCallback
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/api/journal?date=$date&userID=$userID")
            .delete()
            .addHeader("Authorization", "Bearer $googleToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Failed to delete journal!")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess("Journal deleted!")
                } else {
                    callback.onFailure("Error deleting journal!")
                }
            }
        })
    }

    fun sendChatMessage(
        userID: String?,
        googleToken: String?,
        date: String?,
        message: String,
        callback: JournalCallback
    ) {
        val json = JSONObject().apply {
            put("sender", userID)
            put("message", message)
            put("metadata", JSONObject().apply {
                put("date", date)
                put("userID", userID)
                put("google_token", googleToken)
            })
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(chatbotUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Failed to connect to chatbot")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    callback.onSuccess(responseData)
                } else {
                    callback.onFailure("Empty response from chatbot")
                }
            }
        })
    }

    fun convertImageViewToBase64(imageView: ImageView): String? {
        val drawable = imageView.drawable ?: return null
        val bitmap = (drawable as BitmapDrawable).bitmap

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val byteArray = outputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }
}