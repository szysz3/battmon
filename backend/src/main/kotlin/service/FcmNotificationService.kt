package com.battmon.service

import com.battmon.database.DeviceTokenRepository
import com.battmon.model.UpsStatus
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.slf4j.LoggerFactory
import java.io.FileInputStream

class FcmNotificationService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val serviceAccountPath: String,
    private val enabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(FcmNotificationService::class.java)
    private var initialized = false

    fun initialize() {
        if (!enabled) {
            logger.info("FCM notifications are disabled")
            return
        }

        try {
            // Use .use {} to ensure FileInputStream is properly closed
            FileInputStream(serviceAccountPath).use { serviceAccount ->
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options)
                    logger.info("Firebase initialized successfully")
                    initialized = true
                } else {
                    logger.info("Firebase already initialized")
                    initialized = true
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase", e)
            initialized = false
        }
    }

    suspend fun sendStatusAlert(status: UpsStatus) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping notification")
            return
        }

        val notification = buildNotification(status)
        val dataMap = buildStatusDataMap(status)
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "status alert")
    }

    suspend fun sendRecoveryAlert(status: UpsStatus, previousStatus: String?) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping recovery notification")
            return
        }

        val notification = buildRecoveryNotification(status, previousStatus)
        val dataMap = buildStatusDataMap(status)
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "recovery alert")
    }

    suspend fun sendConnectionLostAlert(consecutiveFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection lost notification")
            return
        }

        val notification = buildConnectionLostNotification(consecutiveFailures)
        val dataMap = mapOf(
            "type" to "connection_lost",
            "consecutiveFailures" to consecutiveFailures.toString()
        )
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "connection lost")
    }

    suspend fun sendConnectionRestoredAlert(previousFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection restored notification")
            return
        }

        val notification = buildConnectionRestoredNotification(previousFailures)
        val dataMap = mapOf(
            "type" to "connection_restored",
            "previousFailures" to previousFailures.toString()
        )
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 0, notificationType = "connection restored")
    }

    suspend fun sendTestNotification(token: String): Boolean {
        if (!enabled || !initialized) {
            logger.warn("FCM notifications disabled or not initialized")
            return false
        }

        return try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle("BattMon")
                        .setBody("Test Notification • Push notifications are working correctly")
                        .build()
                )
                .setApnsConfig(
                    ApnsConfig.builder()
                        .setAps(
                            Aps.builder()
                                .setSound("default")
                                .putCustomData("interruption-level", "time-sensitive")
                                .build()
                        )
                        .build()
                )
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(
                            AndroidNotification.builder()
                                .setChannelId("battmon_alerts")
                                .setPriority(AndroidNotification.Priority.HIGH)
                                .setSound("default")
                                .build()
                        )
                        .build()
                )
                .build()

            val response = FirebaseMessaging.getInstance().send(message)
            logger.info("Successfully sent test notification: $response")
            true
        } catch (e: Exception) {
            logger.error("Failed to send test notification", e)
            false
        }
    }

    private fun buildNotification(status: UpsStatus): Notification {
        val subtitle = when {
            status.status.contains("ONBATT", ignoreCase = true) -> "UPS On Battery"
            status.status.contains("LOWBATT", ignoreCase = true) -> "Low Battery Warning"
            status.status.contains("COMMLOST", ignoreCase = true) -> "Communication Lost"
            else -> "Status Alert"
        }

        val bodyParts = mutableListOf<String>()
        bodyParts.add("Status: ${status.status}")

        status.bcharge?.let { bodyParts.add("Battery: ${it.toInt()}%") }
        status.timeleft?.let { bodyParts.add("Time left: ${it.toInt()} min") }
        status.linev?.let { bodyParts.add("Line: ${it.toInt()}V") }

        return Notification.builder()
            .setTitle("BattMon")
            .setBody("$subtitle • ${bodyParts.joinToString(" • ")}")
            .build()
    }

    private fun buildRecoveryNotification(status: UpsStatus, previousStatus: String?): Notification {
        val bodyParts = mutableListOf<String>()
        bodyParts.add("Power Restored")
        bodyParts.add("Status: ${status.status}")
        previousStatus?.let { bodyParts.add("Previous: $it") }

        status.bcharge?.let { bodyParts.add("Battery: ${it.toInt()}%") }
        status.timeleft?.let { bodyParts.add("Time left: ${it.toInt()} min") }
        status.linev?.let { bodyParts.add("Line: ${it.toInt()}V") }

        return Notification.builder()
            .setTitle("BattMon")
            .setBody(bodyParts.joinToString(" • "))
            .build()
    }

    private fun buildConnectionLostNotification(consecutiveFailures: Int): Notification {
        return Notification.builder()
            .setTitle("BattMon")
            .setBody("Connection Lost • Failed to retrieve UPS status $consecutiveFailures times. Check apcupsd connection.")
            .build()
    }

    private fun buildConnectionRestoredNotification(previousFailures: Int): Notification {
        return Notification.builder()
            .setTitle("BattMon")
            .setBody("Connection Restored • Successfully reconnected to apcupsd after $previousFailures failed attempts.")
            .build()
    }

    /**
     * Builds status data map from UpsStatus for inclusion in notifications.
     */
    private fun buildStatusDataMap(status: UpsStatus): Map<String, String> {
        val dataMap = mutableMapOf(
            "status" to status.status,
            "timestamp" to status.timestamp.toString(),
            "upsname" to status.upsname
        )

        status.bcharge?.let { dataMap["bcharge"] = it.toString() }
        status.timeleft?.let { dataMap["timeleft"] = it.toString() }
        status.linev?.let { dataMap["linev"] = it.toString() }
        status.loadpct?.let { dataMap["loadpct"] = it.toString() }

        return dataMap
    }

    /**
     * Generic message builder that constructs a Firebase message with platform-specific configurations.
     * @param notification The notification to send
     * @param dataMap Custom data payload to include with the notification
     * @param fcmToken The device FCM token
     * @param badgeCount iOS badge count (0 to clear, 1+ to show)
     */
    private fun buildMessage(
        notification: Notification,
        dataMap: Map<String, String>,
        fcmToken: String,
        badgeCount: Int
    ): Message {
        return Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putAllData(dataMap)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(badgeCount)
                            .setContentAvailable(true)
                            .putCustomData("interruption-level", "time-sensitive")
                            .build()
                    )
                    .putHeader("apns-priority", "10")
                    .putHeader("apns-push-type", "alert")
                    .build()
            )
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setChannelId("battmon_alerts")
                            .setPriority(AndroidNotification.Priority.HIGH)
                            .setSound("default")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * Generic method to send notifications to all registered device tokens.
     * Handles token validation, error cases, and automatic cleanup of invalid tokens.
     *
     * @param notification The notification content to send
     * @param dataMap Custom data payload to include with the notification
     * @param badgeCount iOS badge count (0 to clear, 1+ to show)
     * @param notificationType Description of notification type for logging (e.g., "status alert", "connection lost")
     */
    private suspend fun sendNotificationToAllTokens(
        notification: Notification,
        dataMap: Map<String, String>,
        badgeCount: Int,
        notificationType: String
    ) {
        val tokens = deviceTokenRepository.findAll()
        if (tokens.isEmpty()) {
            logger.debug("No device tokens registered, skipping $notificationType notification")
            return
        }

        tokens.forEach { deviceToken ->
            try {
                val message = buildMessage(notification, dataMap, deviceToken.fcmToken, badgeCount)
                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent $notificationType notification to ${deviceToken.deviceName ?: "unknown device"}: $response")
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.warn("Invalid or unregistered token for ${deviceToken.deviceName}, removing: ${e.message}")
                        deviceTokenRepository.delete(deviceToken.fcmToken)
                    }
                    else -> {
                        logger.error("Failed to send $notificationType notification to ${deviceToken.deviceName}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected error sending $notificationType notification to ${deviceToken.deviceName}", e)
            }
        }
    }
}
