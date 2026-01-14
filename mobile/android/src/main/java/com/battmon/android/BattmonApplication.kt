package com.battmon.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp

class BattmonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Log.d(TAG, "Firebase initialized")

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BattMon Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical UPS status alerts and battery notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created with HIGH importance")
        }
    }

    companion object {
        private const val TAG = "BattmonApplication"
        const val CHANNEL_ID = "battmon_alerts"
    }
}
