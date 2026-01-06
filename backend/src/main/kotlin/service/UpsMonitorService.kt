package com.battmon.service

import com.battmon.database.UpsStatusRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class UpsMonitorService(
    private val repository: UpsStatusRepository,
    private val pollIntervalSeconds: Long = 5,
    private val apcAccessCommand: String = "apcaccess status"
) {
    private val logger = LoggerFactory.getLogger(UpsMonitorService::class.java)
    private var monitorJob: Job? = null

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
        val output = executeApcAccess()
        val status = ApcAccessParser.parse(output)
        repository.insert(status)
        logger.debug("Stored UPS status: ${status.status}")
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
