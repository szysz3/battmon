package com.battmon.service

import com.battmon.database.UpsStatusRepository
import com.battmon.model.UpsStatus
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class UpsMonitorService(
    private val repository: UpsStatusRepository,
    private val pollIntervalSeconds: Long = 5,
    private val apcAccessCommand: String = "apcaccess status",
    private val fcmService: FcmNotificationService? = null
) {
    private val logger = LoggerFactory.getLogger(UpsMonitorService::class.java)
    private var monitorJob: Job? = null
    private var lastStatus: String? = null
    private var consecutiveFailures: Int = 0
    private var failureNotificationSent: Boolean = false

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 10
    }

    fun start(scope: CoroutineScope) {
        if (monitorJob?.isActive == true) {
            logger.warn("UPS monitor is already running")
            return
        }

        logger.info("Starting UPS monitor with ${pollIntervalSeconds}s interval")

        monitorJob = scope.launch {
            while (isActive) {
                try {
                    pollAndStore()
                } catch (e: Exception) {
                    logger.error("Error polling UPS status", e)
                }

                delay(pollIntervalSeconds * 1000)
            }
        }
    }

    fun stop() {
        logger.info("Stopping UPS monitor")
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun pollAndStore() = withContext(Dispatchers.IO) {
        try {
            val output = executeApcAccess()
            val status = ApcAccessParser.parse(output)

            // Validate that the status has meaningful data
            if (isBlankStatus(status)) {
                logger.warn("Received blank/empty status data from apcaccess, not storing")
                handleFailure()
                return@withContext
            }

            // Valid data received - store it and reset failure counter
            repository.insert(status)
            logger.debug("Stored UPS status: ${status.status}")

            // Reset failure tracking on successful data retrieval
            if (consecutiveFailures > 0) {
                logger.info("Successfully received data after $consecutiveFailures failures")

                // Send recovery notification if we previously sent a failure notification
                if (failureNotificationSent) {
                    sendConnectionRestoredNotification(consecutiveFailures)
                }

                consecutiveFailures = 0
                failureNotificationSent = false
            }

            // Check for status changes and send notifications
            checkStatusAndNotify(status)

        } catch (e: Exception) {
            logger.error("Failed to poll UPS status", e)
            handleFailure()
        }
    }

    private suspend fun handleFailure() {
        consecutiveFailures++
        logger.warn("Consecutive failures: $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES")

        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES && !failureNotificationSent) {
            logger.error("Reached $MAX_CONSECUTIVE_FAILURES consecutive failures - sending notification")
            sendFailureNotification()
            failureNotificationSent = true
        }
    }

    private suspend fun sendFailureNotification() {
        fcmService?.sendConnectionLostAlert(consecutiveFailures)
    }

    private suspend fun sendConnectionRestoredNotification(previousFailures: Int) {
        fcmService?.sendConnectionRestoredAlert(previousFailures)
    }

    private fun isBlankStatus(status: UpsStatus): Boolean {
        // Consider status blank if critical fields are empty or missing
        return status.status.isBlank() ||
               status.apc.isBlank() ||
               status.hostname.isBlank() ||
               status.model.isBlank()
    }

    private suspend fun checkStatusAndNotify(status: UpsStatus) {
        val currentStatus = status.status.uppercase()
        val previousStatus = lastStatus

        // Check if status changed to non-ONLINE
        val isCurrentlyOffline = !currentStatus.contains("ONLINE")
        val wasOnlineOrUnknown = previousStatus == null || previousStatus.contains("ONLINE")

        if (isCurrentlyOffline && wasOnlineOrUnknown) {
            logger.warn("UPS status changed from ${previousStatus ?: "unknown"} to $currentStatus - sending notification")
            fcmService?.sendStatusAlert(status)
        } else if (isCurrentlyOffline && !wasOnlineOrUnknown) {
            logger.debug("UPS still offline: $currentStatus (was: $previousStatus)")
        } else if (!isCurrentlyOffline && previousStatus != null && !previousStatus.contains("ONLINE")) {
            logger.info("UPS status recovered from $previousStatus to $currentStatus")
            fcmService?.sendRecoveryAlert(status, previousStatus)
        }

        lastStatus = currentStatus
    }

    private fun executeApcAccess(): String {
        val process = ProcessBuilder(apcAccessCommand.split(" "))
            .redirectErrorStream(true)
            .start()

        val output = BufferedReader(InputStreamReader(process.inputStream))
            .use { it.readText() }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("apcaccess failed (exit=$exitCode): ${output.trim()}")
        }
        return output
    }
}
