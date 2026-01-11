package com.battmon.notifications

import com.battmon.data.api.BattmonApi
import com.battmon.model.DeviceTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Platform-agnostic notification token registration service.
 * Call registerToken() with the FCM token received from the platform-specific layer.
 */
class NotificationTokenManager(
    private val api: BattmonApi = BattmonApi()
) {
    /**
     * Register an FCM token with the backend.
     * This should be called from platform-specific code (Swift on iOS, Kotlin on Android)
     * after receiving the FCM token.
     */
    fun registerToken(fcmToken: String, deviceName: String? = null, platform: String = "ios") {
        println("Registering FCM token: $fcmToken")
        CoroutineScope(Dispatchers.Default).launch {
            val success = api.registerDeviceToken(
                DeviceTokenRequest(
                    fcmToken = fcmToken,
                    deviceName = deviceName,
                    platform = platform
                )
            )
            if (success) {
                println("Device token registered successfully with backend")
            } else {
                println("Failed to register device token with backend")
            }
        }
    }

    /**
     * Unregister an FCM token from the backend.
     */
    fun unregisterToken(fcmToken: String) {
        println("Unregistering FCM token: $fcmToken")
        CoroutineScope(Dispatchers.Default).launch {
            val success = api.unregisterDeviceToken(fcmToken)
            if (success) {
                println("Device token unregistered successfully")
            } else {
                println("Failed to unregister device token")
            }
        }
    }
}
