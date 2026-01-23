package com.battmon.service

import com.battmon.database.DeviceTokenRepository
import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.FileInputStream

class FcmNotificationService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val serviceAccountPath: String,
    private val enabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(FcmNotificationService::class.java)
    private var initialized = false

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
    }

    fun initialize() {
        if (!enabled) {
            logger.info("FCM notifications are disabled")
            return
        }

        try {
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

    // ==================== Multi-Device Methods ====================

    suspend fun sendStatusAlert(device: UpsDevice, status: UpsStatus) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping notification")
            return
        }

        val notification = buildNotification(device, status)
        val dataMap = buildStatusDataMap(device, status)
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "status alert for ${device.name}")
    }

    suspend fun sendRecoveryAlert(device: UpsDevice, status: UpsStatus, previousStatus: String?) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping recovery notification")
            return
        }

        val notification = buildRecoveryNotification(device, status, previousStatus)
        val dataMap = buildStatusDataMap(device, status)
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "recovery alert for ${device.name}")
    }

    suspend fun sendConnectionLostAlert(device: UpsDevice, consecutiveFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection lost notification")
            return
        }

        val notification = buildConnectionLostNotification(device, consecutiveFailures)
        val dataMap = mapOf(
            "type" to "connection_lost",
            "deviceId" to device.id,
            "deviceName" to device.name,
            "consecutiveFailures" to consecutiveFailures.toString()
        )
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 1, notificationType = "connection lost for ${device.name}")
    }

    suspend fun sendConnectionRestoredAlert(device: UpsDevice, previousFailures: Int) {
        if (!enabled || !initialized) {
            logger.debug("FCM notifications disabled or not initialized, skipping connection restored notification")
            return
        }

        val notification = buildConnectionRestoredNotification(device, previousFailures)
        val dataMap = mapOf(
            "type" to "connection_restored",
            "deviceId" to device.id,
            "deviceName" to device.name,
            "previousFailures" to previousFailures.toString()
        )
        sendNotificationToAllTokens(notification, dataMap, badgeCount = 0, notificationType = "connection restored for ${device.name}")
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

            val response = withContext(Dispatchers.IO) {
                FirebaseMessaging.getInstance().send(message)
            }
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

    // ==================== Multi-Device Notification Builders ====================

    private fun buildNotification(device: UpsDevice, status: UpsStatus): Notification {
        val subtitle = when {
            status.status.contains("ONBATT", ignoreCase = true) -> "On Battery"
            status.status.contains("LOWBATT", ignoreCase = true) -> "Low Battery"
            status.status.contains("COMMLOST", ignoreCase = true) -> "Communication Lost"
            else -> "Status Alert"
        }

        val locationInfo = device.location?.let { " ($it)" } ?: ""
        val bodyParts = mutableListOf<String>()
        bodyParts.add("${device.name}$locationInfo: $subtitle")

        status.bcharge?.let { bodyParts.add("Battery: ${it.toInt()}%") }
        status.timeleft?.let { bodyParts.add("Time: ${it.toInt()} min") }

        return Notification.builder()
            .setTitle("BattMon")
            .setBody(bodyParts.joinToString(" • "))
            .build()
    }

    private fun buildRecoveryNotification(device: UpsDevice, status: UpsStatus, previousStatus: String?): Notification {
        val locationInfo = device.location?.let { " ($it)" } ?: ""
        val bodyParts = mutableListOf<String>()
        bodyParts.add("${device.name}$locationInfo: Power Restored")
        bodyParts.add("Status: ${status.status}")

        status.bcharge?.let { bodyParts.add("Battery: ${it.toInt()}%") }

        return Notification.builder()
            .setTitle("BattMon")
            .setBody(bodyParts.joinToString(" • "))
            .build()
    }

    private fun buildConnectionLostNotification(device: UpsDevice, consecutiveFailures: Int): Notification {
        val locationInfo = device.location?.let { " ($it)" } ?: ""
        return Notification.builder()
            .setTitle("BattMon")
            .setBody("${device.name}$locationInfo: Connection Lost • Failed $consecutiveFailures times at ${device.host}:${device.port}")
            .build()
    }

    private fun buildConnectionRestoredNotification(device: UpsDevice, previousFailures: Int): Notification {
        val locationInfo = device.location?.let { " ($it)" } ?: ""
        return Notification.builder()
            .setTitle("BattMon")
            .setBody("${device.name}$locationInfo: Connection Restored • Reconnected after $previousFailures failures")
            .build()
    }

    private fun buildStatusDataMap(device: UpsDevice, status: UpsStatus): Map<String, String> {
        val dataMap = mutableMapOf(
            "type" to "status_alert",
            "deviceId" to device.id,
            "deviceName" to device.name,
            "status" to status.status,
            "timestamp" to status.timestamp.toString(),
            "upsname" to status.upsname
        )

        device.location?.let { dataMap["deviceLocation"] = it }
        status.bcharge?.let { dataMap["bcharge"] = it.toString() }
        status.timeleft?.let { dataMap["timeleft"] = it.toString() }
        status.linev?.let { dataMap["linev"] = it.toString() }
        status.loadpct?.let { dataMap["loadpct"] = it.toString() }

        return dataMap
    }

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
            val deviceName = deviceToken.deviceName ?: "unknown device"
            val message = buildMessage(notification, dataMap, deviceToken.fcmToken, badgeCount)

            sendMessageWithRetry(message, deviceToken.fcmToken, deviceName, notificationType)
        }
    }

    private suspend fun sendMessageWithRetry(
        message: Message,
        fcmToken: String,
        deviceName: String,
        notificationType: String
    ) {
        var lastException: Exception? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = withContext(Dispatchers.IO) {
                    FirebaseMessaging.getInstance().send(message)
                }
                logger.info("Successfully sent $notificationType notification to $deviceName: $response")
                return
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.UNREGISTERED -> {
                        logger.warn("Invalid or unregistered token for $deviceName, removing: ${e.message}")
                        deviceTokenRepository.delete(fcmToken)
                        return
                    }
                    else -> {
                        lastException = e
                        if (attempt < MAX_RETRIES - 1) {
                            val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                            logger.warn("Transient error sending $notificationType to $deviceName (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${delayMs}ms: ${e.message}")
                            delay(delayMs)
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl attempt)
                    logger.warn("Unexpected error sending $notificationType to $deviceName (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }

        logger.error("Failed to send $notificationType notification to $deviceName after $MAX_RETRIES attempts", lastException)
    }
}
