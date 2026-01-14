package com.battmon.service

import com.battmon.database.UpsStatusRepository
import com.battmon.model.UpsStatus
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class UpsMonitorService(
    private val repository: UpsStatusRepository,
    private val pollIntervalSeconds: Long = 10,
    private val apcAccessCommand: String = "apcaccess status",
    private val fcmService: FcmNotificationService? = null,
    private val emailService: EmailNotificationService? = null
) {
    private val logger = LoggerFactory.getLogger(UpsMonitorService::class.java)
    private var monitorJob: Job? = null
    private var lastStatus: String? = null
    private var lastApcAccessOutput: String? = null
    private var consecutiveFailures: Int = 0
    private var failureNotificationSent: Boolean = false

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 10
        private const val APCACCESS_TIMEOUT_SECONDS = 7L
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
                    sendConnectionRestoredNotification(consecutiveFailures, output)
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
            sendFailureNotification()
            failureNotificationSent = true
        }
    }

    private suspend fun sendFailureNotification() {
        fcmService?.sendConnectionLostAlert(consecutiveFailures)
        emailService?.sendConnectionLostEmail(consecutiveFailures, lastApcAccessOutput)
    }

    private suspend fun sendConnectionRestoredNotification(previousFailures: Int, currentOutput: String) {
        fcmService?.sendConnectionRestoredAlert(previousFailures)
        emailService?.sendConnectionRestoredEmail(previousFailures, currentOutput)
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
            fcmService?.sendStatusAlert(status)
            emailService?.sendStatusAlertEmail(status, apcAccessOutput)
        } else if (isCurrentlyOffline && !wasOnlineOrUnknown) {
            logger.debug("UPS still offline: $currentStatus (was: $previousStatus)")
        } else if (!isCurrentlyOffline && previousStatus != null && !previousStatus.contains("ONLINE")) {
            logger.info("UPS status recovered from $previousStatus to $currentStatus")
            fcmService?.sendRecoveryAlert(status, previousStatus)
            emailService?.sendRecoveryEmail(status, previousStatus, apcAccessOutput)
        }

        lastStatus = currentStatus
    }

    private fun executeApcAccess(): String {
        val commandParts = parseCommand(apcAccessCommand)
        val process = ProcessBuilder(commandParts)
            .redirectErrorStream(true)
            .start()

        try {
            val output = process.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }

            val completed = process.waitFor(APCACCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                throw IllegalStateException("apcaccess timed out after ${APCACCESS_TIMEOUT_SECONDS}s")
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                throw IllegalStateException("apcaccess failed (exit=$exitCode): ${output.trim()}")
            }
            return output
        } finally {
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
            }
        }
    }

    private fun parseCommand(command: String): List<String> {
        val tokenRegex = Regex("""("[^"]*"|'[^']*'|\S+)""")
        return tokenRegex.findAll(command)
            .map { match ->
                match.value.trim().removeSurrounding("\"").removeSurrounding("'")
            }
            .toList()
            .ifEmpty { listOf(command) }
    }
}
