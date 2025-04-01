// ProfileApiClient.kt
package com.example.cpen321project.profile

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProfileApiClient {
    private val client = OkHttpClient()
    private val BASE_URL = "https://cpen321project-journal.duckdns.org"

    interface ProfileCallback {
        fun onSuccess(response: String)
        fun onFailure(error: String)
    }

    fun getProfile(userID: String?, callback: ProfileCallback) {
        val request = Request.Builder()
            .url("$BASE_URL/api/profile?userID=$userID")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        callback.onSuccess(it)
                    } ?: callback.onFailure("Empty response")
                } else {
                    callback.onFailure("Failed to get profile: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Connection error: ${e.message}")
            }
        })
    }

    fun updateProfile(
        userID: String?,
        googleToken: String?,
        preferredName: String,
        activities: List<Activity>,
        callback: ProfileCallback
    ) {
        val json = JSONObject().apply {
            put("userID", userID)
            put("preferred_name", preferredName)
            put("googleToken", googleToken)

            val activitiesArray = JSONArray()
            for (activity in activities) {
                val activityJson = JSONObject().apply {
                    put("name", activity.name)
                    put("averageValue", activity.averageValue)
                    put("unit", activity.unit)
                }
                activitiesArray.put(activityJson)
            }
            put("activities_tracking", activitiesArray)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/api/profile")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess("Profile updated successfully")
                } else {
                    callback.onFailure("Failed to update profile: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Connection error: ${e.message}")
            }
        })
    }

    fun updateReminder(
        userID: String?,
        weekdays: List<Int>,
        time: String,
        callback: ProfileCallback
    ) {
        val json = JSONObject().apply {
            put("userID", userID)

            val updatedReminder = JSONObject().apply {
                put("Weekday", JSONArray(weekdays))
                put("time", time)
            }
            put("updated_reminder", updatedReminder)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/api/profile/reminder")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    callback.onSuccess("Reminder updated successfully")
                } else {
                    callback.onFailure("Failed to update reminder: ${response.code}")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure("Connection error: ${e.message}")
            }
        })
    }

    fun getPaymentSheet(userID: String?, callback: (PaymentSheetData?) -> Unit) {
        val json = JSONObject().apply {
            put("userID", userID)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$BASE_URL/api/payment-sheet")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseBody ->
                        val jsonResponse = JSONObject(responseBody)
                        val data = PaymentSheetData(
                            paymentIntent = jsonResponse.getString("paymentIntent"),
                            customerId = jsonResponse.getString("customer"),
                            ephemeralKey = jsonResponse.getString("ephemeralKey"),
                            publishableKey = jsonResponse.getString("publishableKey")
                        )
                        callback(data)
                    } ?: callback(null)
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
        })
    }
}

data class PaymentSheetData(
    val paymentIntent: String,
    val customerId: String,
    val ephemeralKey: String,
    val publishableKey: String
)