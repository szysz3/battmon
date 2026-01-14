package com.battmon.notifications

import com.battmon.data.api.BattmonApi
import com.battmon.model.DeviceTokenRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationTokenManager(
    private val api: BattmonApi = BattmonApi(),
    private val scope: CoroutineScope = defaultScope()
) {
    constructor(api: BattmonApi) : this(api, defaultScope())

    companion object {
        private fun defaultScope(): CoroutineScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
                println("NotificationTokenManager: Uncaught exception: ${throwable.message}")
            }
        )
    }
    fun registerToken(fcmToken: String, deviceName: String? = null, platform: String = "ios") {
        println("Registering FCM token: $fcmToken")
        scope.launch {
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

    fun unregisterToken(fcmToken: String) {
        println("Unregistering FCM token: $fcmToken")
        scope.launch {
            val success = api.unregisterDeviceToken(fcmToken)
            if (success) {
                println("Device token unregistered successfully")
            } else {
                println("Failed to unregister device token")
            }
        }
    }
}
