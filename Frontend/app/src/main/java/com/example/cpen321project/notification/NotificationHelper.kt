package com.example.cpen321project.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    const val CHANNEL_ID = "channel_id"
    private val permissionsArr = arrayOf(Manifest.permission.POST_NOTIFICATIONS)

    fun setupNotificationChannel(context: Context) {
        // Check and request notification permission
        if (ContextCompat.checkSelfPermission(context, permissionsArr[0]) != PackageManager.PERMISSION_GRANTED) {
            if (context is AppCompatActivity) {
                ActivityCompat.requestPermissions(context, permissionsArr, 200)
            }
        }

        // Create notification channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getNotificationManager(context: Context): NotificationManager {
        return ContextCompat.getSystemService(context, NotificationManager::class.java)!!
    }
}