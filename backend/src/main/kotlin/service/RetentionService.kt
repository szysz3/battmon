package com.battmon.service

import com.battmon.database.UpsStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory

class RetentionService(
    private val repository: UpsStatusRepository,
    private val retentionDays: Int,
    private val cleanupIntervalHours: Long
) {
    private val logger = LoggerFactory.getLogger(RetentionService::class.java)
    private var cleanupJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (cleanupJob?.isActive == true) {
            logger.warn("Retention job is already running")
            return
        }

        logger.info("Starting retention job: keep ${retentionDays}d, run every ${cleanupIntervalHours}h")
        cleanupJob = scope.launch {
            while (isActive) {
                try {
                    cleanupOnce()
                } catch (e: Exception) {
                    logger.error("Error running retention cleanup", e)
                }

                delay(cleanupIntervalHours * 60 * 60 * 1000)
            }
        }
    }

    fun stop() {
        logger.info("Stopping retention job")
        cleanupJob?.cancel()
        cleanupJob = null
    }

    private suspend fun cleanupOnce() = withContext(Dispatchers.IO) {
        val cutoff = retentionCutoff()
        val deleted = repository.deleteOlderThan(cutoff)
        if (deleted > 0) {
            logger.info("Retention cleanup deleted $deleted rows older than $cutoff")
        }
    }

    private fun retentionCutoff(): Instant {
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val cutoffMillis = nowMillis - retentionDays.toLong() * 24 * 60 * 60 * 1000
        return Instant.fromEpochMilliseconds(cutoffMillis)
    }
}
