package com.battmon.android

import android.util.Log
import com.battmon.data.api.BattmonApi
import com.battmon.notifications.NotificationTokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BattmonFirebaseMessagingService : FirebaseMessagingService() {

    private val tokenManager by lazy {
        NotificationTokenManager(api = BattmonApi())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")

        // Get device name
        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        // Register token with backend
        tokenManager.registerToken(
            fcmToken = token,
            deviceName = deviceName,
            platform = "android"
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Handle notification payload
        message.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
        }

        // Handle data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
        }
    }

    companion object {
        private const val TAG = "BattmonFCMService"
    }
}
