package com.example.cpen321project

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cpen321project.BuildConfig.WEB_CLIENT_ID
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import com.example.cpen321project.MainActivity.Companion
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import android.util.Base64

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "login Activity"
    }


    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.loginButton).setOnClickListener() {
            Log.d("auth", "WEB CLIENT ID: ${BuildConfig.WEB_CLIENT_ID}")
            Log.d(TAG, "Sign in button clicked")
            Log.d(TAG, "WEB CLIENT ID: ${BuildConfig.WEB_CLIENT_ID}")


            val credentialManager = CredentialManager.create(this)
            val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
                .Builder(BuildConfig.WEB_CLIENT_ID)
                .setNonce("")
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            activityScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = this@LoginActivity,
                    )
                    handleSignIn(result)
                } catch (e: GetCredentialException) {
                    handleFailure(e)
                }
            }
        }
    }

    fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        Log.d(TAG, "Google ID token: ${googleIdTokenCredential.idToken}")
                        Log.d(TAG, "Google id: ${googleIdTokenCredential.id}")
                        Log.d(TAG, "credential: ${credential.data}")
                        Log.d(TAG, "result: ${credential}")
                        var googleUserId = googleIdTokenCredential.id
//                        googleUserId = "440008"
                        val google_num_id = getGoogleUserIDFromIdToken(googleIdTokenCredential.idToken)

                        // Save Google User ID in SharedPreferences
                        getSharedPreferences("AppPreferences", MODE_PRIVATE)
                            .edit()
                            .putString("GoogleUserID", googleUserId)
                            .putString("GoogleIDtoken", googleIdTokenCredential.idToken)
                            .putString("google_num_id", google_num_id)
                            .apply()

                        // post a new user
                        callCreateUser(googleUserId, googleIdTokenCredential.idToken)

                        // Redirect to MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("GoogleUserID", googleUserId)
                        startActivity(intent)
                        finish() // Close LoginActivity
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                }
                else {
                    // Catch any unrecognized credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    fun handleFailure(e: GetCredentialException) {
        Log.e(TAG, "Error getting credentials")
    }

    private fun callCreateUser(userID: String, googleToken: String) {
        Log.d(TAG, "creating user ${userID}")
//        val userID = "12345" // Get this dynamically (e.g., after user login)
        val url = "https://cpen321project-journal.duckdns.org/api/profile"
//        val url = "http://10.0.2.2:3001/api/profile/fcmtoken"
        val json = JSONObject()
        json.put("userID", userID)
        json.put("googleToken", googleToken)

        val client = OkHttpClient()
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(), json.toString()
        )
        Log.d(TAG, "Sending ${googleToken}")

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Response: ${response.code} - ${response.message}")

                    // Safely read response body as String
                    val responseBody = response.body?.string() ?: "No Response Body"
                    Log.d(TAG, "Response Body: $responseBody")
                } else {
                    Log.e(TAG, "Failed to create user. Status Code: ${response.code}")
                    val responseBody = response.body?.string() ?: "No Response Body"
                    Log.e(TAG, "Response Body: $responseBody")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to create user", e)
            }
        })

    }

    fun getGoogleUserIDFromIdToken(idToken: String): String? {
        // Split the token into Header, Payload, and Signature
        val parts = idToken.split(".")
        if (parts.size != 3) {
            // Invalid ID Token
            return null
        }

        try {
            // Decode the Payload (2nd part) from Base64 URL Safe format
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            val jsonObject = JSONObject(payload)

            // Extract the 'sub' claim which is the Google User ID
            val google_num_id = jsonObject.getString("sub")
            Log.d(TAG, "google num id: ${google_num_id}")
            return google_num_id
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

}