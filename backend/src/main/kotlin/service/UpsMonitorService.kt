package com.battmon.service

import com.battmon.database.UpsStatusRepository
import com.battmon.model.UpsStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class UpsMonitorService(
    private val repository: UpsStatusRepository,
    private val apcAccessClient: ApcAccessClient,
    private val notificationDispatcher: UpsNotificationDispatcher,
    private val pollIntervalSeconds: Long = 10
) {
    private val logger = LoggerFactory.getLogger(UpsMonitorService::class.java)
    private var monitorJob: Job? = null
    private var lastStatus: String? = null
    private var lastApcAccessOutput: String? = null
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
            val output = apcAccessClient.fetchStatusOutput()
            val status = ApcAccessParser.parse(output)

            if (isBlankStatus(status)) {
                logger.warn("Received blank/empty status data from apcaccess, not storing")
                handleFailure()
                return@withContext
            }

            repository.insert(status)
            logger.debug("Stored UPS status: ${status.status}")

            lastApcAccessOutput = output

            if (consecutiveFailures > 0) {
                logger.info("Successfully received data after $consecutiveFailures failures")

                if (failureNotificationSent) {
                    notificationDispatcher.sendConnectionRestoredAlert(consecutiveFailures, output)
                }

                consecutiveFailures = 0
                failureNotificationSent = false
            }

            checkStatusAndNotify(status, output)

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
            notificationDispatcher.sendConnectionLostAlert(consecutiveFailures, lastApcAccessOutput)
            failureNotificationSent = true
        }
    }

    private fun isBlankStatus(status: UpsStatus): Boolean {
        return status.status.isBlank() ||
               status.apc.isBlank() ||
               status.hostname.isBlank() ||
               status.model.isBlank()
    }

    private suspend fun checkStatusAndNotify(status: UpsStatus, apcAccessOutput: String) {
        val currentStatus = status.status.uppercase()
        val previousStatus = lastStatus

        val isCurrentlyOffline = !currentStatus.contains("ONLINE")
        val wasOnlineOrUnknown = previousStatus == null || previousStatus.contains("ONLINE")

        if (isCurrentlyOffline && wasOnlineOrUnknown) {
            logger.warn("UPS status changed from ${previousStatus ?: "unknown"} to $currentStatus - sending notification")
            notificationDispatcher.sendStatusAlert(status, apcAccessOutput)
        } else if (isCurrentlyOffline && !wasOnlineOrUnknown) {
            logger.debug("UPS still offline: $currentStatus (was: $previousStatus)")
        } else if (!isCurrentlyOffline && previousStatus != null && !previousStatus.contains("ONLINE")) {
            logger.info("UPS status recovered from $previousStatus to $currentStatus")
            notificationDispatcher.sendRecoveryAlert(status, previousStatus, apcAccessOutput)
        }

        lastStatus = currentStatus
    }
}
