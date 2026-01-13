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
            val serviceAccount = FileInputStream(serviceAccountPath)
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
        sendNotificationToAllTokens(notification, status)
    }

    suspend fun sendRecoveryAlert(status: UpsStatus, previousStatus: String?) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping recovery notification")
            return
        }

        val notification = buildRecoveryNotification(status, previousStatus)
        sendNotificationToAllTokens(notification, status)
    }

    suspend fun sendConnectionLostAlert(consecutiveFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection lost notification")
            return
        }

        val notification = buildConnectionLostNotification(consecutiveFailures)
        sendConnectionLostNotificationToAllTokens(notification, consecutiveFailures)
    }

    suspend fun sendConnectionRestoredAlert(previousFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection restored notification")
            return
        }

        val notification = buildConnectionRestoredNotification(previousFailures)
        sendConnectionRestoredNotificationToAllTokens(notification, previousFailures)
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

    private fun buildMessage(notification: Notification, status: UpsStatus, fcmToken: String): Message {
        val dataMap = mutableMapOf(
            "status" to status.status,
            "timestamp" to status.timestamp.toString(),
            "upsname" to status.upsname
        )

        status.bcharge?.let { dataMap["bcharge"] = it.toString() }
        status.timeleft?.let { dataMap["timeleft"] = it.toString() }
        status.linev?.let { dataMap["linev"] = it.toString() }
        status.loadpct?.let { dataMap["loadpct"] = it.toString() }

        return Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putAllData(dataMap)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(1)
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

    private suspend fun sendNotificationToAllTokens(notification: Notification, status: UpsStatus) {
        val tokens = deviceTokenRepository.findAll()
        if (tokens.isEmpty()) {
            logger.debug("No device tokens registered, skipping notification")
            return
        }

        tokens.forEach { deviceToken ->
            try {
                val message = buildMessage(notification, status, deviceToken.fcmToken)
                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent notification to ${deviceToken.deviceName ?: "unknown device"}: $response")
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.warn("Invalid or unregistered token for ${deviceToken.deviceName}, removing: ${e.message}")
                        deviceTokenRepository.delete(deviceToken.fcmToken)
                    }
                    else -> {
                        logger.error("Failed to send notification to ${deviceToken.deviceName}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected error sending notification to ${deviceToken.deviceName}", e)
            }
        }
    }

    private suspend fun sendConnectionLostNotificationToAllTokens(notification: Notification, consecutiveFailures: Int) {
        val tokens = deviceTokenRepository.findAll()
        if (tokens.isEmpty()) {
            logger.debug("No device tokens registered, skipping connection lost notification")
            return
        }

        tokens.forEach { deviceToken ->
            try {
                val message = buildConnectionLostMessage(notification, consecutiveFailures, deviceToken.fcmToken)
                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent connection lost notification to ${deviceToken.deviceName ?: "unknown device"}: $response")
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.warn("Invalid or unregistered token for ${deviceToken.deviceName}, removing: ${e.message}")
                        deviceTokenRepository.delete(deviceToken.fcmToken)
                    }
                    else -> {
                        logger.error("Failed to send connection lost notification to ${deviceToken.deviceName}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected error sending connection lost notification to ${deviceToken.deviceName}", e)
            }
        }
    }

    private suspend fun sendConnectionRestoredNotificationToAllTokens(notification: Notification, previousFailures: Int) {
        val tokens = deviceTokenRepository.findAll()
        if (tokens.isEmpty()) {
            logger.debug("No device tokens registered, skipping connection restored notification")
            return
        }

        tokens.forEach { deviceToken ->
            try {
                val message = buildConnectionRestoredMessage(notification, previousFailures, deviceToken.fcmToken)
                val response = FirebaseMessaging.getInstance().send(message)
                logger.info("Successfully sent connection restored notification to ${deviceToken.deviceName ?: "unknown device"}: $response")
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.warn("Invalid or unregistered token for ${deviceToken.deviceName}, removing: ${e.message}")
                        deviceTokenRepository.delete(deviceToken.fcmToken)
                    }
                    else -> {
                        logger.error("Failed to send connection restored notification to ${deviceToken.deviceName}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected error sending connection restored notification to ${deviceToken.deviceName}", e)
            }
        }
    }

    private fun buildConnectionLostMessage(notification: Notification, consecutiveFailures: Int, fcmToken: String): Message {
        val dataMap = mapOf(
            "type" to "connection_lost",
            "consecutiveFailures" to consecutiveFailures.toString()
        )

        return Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putAllData(dataMap)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(1)
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

    private fun buildConnectionRestoredMessage(notification: Notification, previousFailures: Int, fcmToken: String): Message {
        val dataMap = mapOf(
            "type" to "connection_restored",
            "previousFailures" to previousFailures.toString()
        )

        return Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putAllData(dataMap)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound("default")
                            .setBadge(0)
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
}
