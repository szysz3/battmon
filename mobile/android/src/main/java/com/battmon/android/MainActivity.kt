package com.battmon.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.battmon.App
import com.battmon.data.api.BattmonApi
import com.battmon.notifications.NotificationTokenManager
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val tokenManager by lazy {
        NotificationTokenManager(api = BattmonApi())
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            fetchAndRegisterToken()
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionAndToken()

        setContent {
            App()
        }
    }

    private fun requestNotificationPermissionAndToken() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                    fetchAndRegisterToken()
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            Log.d(TAG, "Notification permission not required for this Android version")
            fetchAndRegisterToken()
        }
    }

    private fun fetchAndRegisterToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM token: $token")

            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            tokenManager.registerToken(
                fcmToken = token,
                deviceName = deviceName,
                platform = "android"
            )
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
