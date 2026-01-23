package com.battmon.service

import com.battmon.database.UpsDeviceRepository
import com.battmon.database.UpsStatusRepository
import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for monitoring multiple UPS devices concurrently.
 *
 * Each enabled device gets its own polling coroutine that runs independently.
 * Failure of one device doesn't affect others.
 */
class MultiUpsMonitorService(
    private val deviceRepository: UpsDeviceRepository,
    private val statusRepository: UpsStatusRepository,
    private val apcAccessClient: ApcAccessClient,
    private val notificationDispatcher: MultiDeviceNotificationDispatcher,
    private val pollIntervalSeconds: Long = 10,
    private val failureThreshold: Int = 10
) {
    private val logger = LoggerFactory.getLogger(MultiUpsMonitorService::class.java)
    private val deviceMonitors = ConcurrentHashMap<String, DeviceMonitorJob>()
    private var serviceScope: CoroutineScope? = null
    private var isRunning = false

    /**
     * Start the monitoring service.
     * Loads all enabled devices and starts monitoring each one.
     */
    fun start(scope: CoroutineScope) {
        if (isRunning) {
            logger.warn("MultiUpsMonitorService is already running")
            return
        }

        logger.info("Starting MultiUpsMonitorService with ${pollIntervalSeconds}s poll interval")
        serviceScope = scope
        isRunning = true

        scope.launch {
            loadAndStartMonitoring()
        }
    }

    /**
     * Stop the monitoring service.
     * Cancels all device monitoring coroutines and waits for cleanup.
     */
    fun stop() {
        if (!isRunning) {
            logger.warn("MultiUpsMonitorService is not running")
            return
        }

        logger.info("Stopping MultiUpsMonitorService")
        isRunning = false

        // Cancel all jobs and collect them for joining
        val jobs = deviceMonitors.values.map { monitor ->
            monitor.job.also { it.cancel() }
        }

        // Wait for all jobs to complete with timeout
        runBlocking {
            withTimeoutOrNull(SHUTDOWN_TIMEOUT_MS) {
                jobs.forEach { it.join() }
            } ?: logger.warn("Some monitor jobs did not complete within ${SHUTDOWN_TIMEOUT_MS}ms timeout")
        }

        deviceMonitors.clear()
        serviceScope = null

        logger.info("MultiUpsMonitorService stopped")
    }

    companion object {
        private const val SHUTDOWN_TIMEOUT_MS = 5000L
    }

    /**
     * Add a new device to monitoring.
     * Called when a device is created via API.
     * Uses atomic putIfAbsent to prevent race conditions.
     */
    suspend fun addDevice(device: UpsDevice) {
        if (!isRunning) {
            logger.debug("Service not running, skipping add for device ${device.id}")
            return
        }

        if (!device.enabled) {
            logger.debug("Device ${device.id} is disabled, not starting monitor")
            return
        }

        startMonitoringIfAbsent(device)
    }

    /**
     * Remove a device from monitoring.
     * Called when a device is deleted via API.
     */
    fun removeDevice(deviceId: String) {
        stopMonitoring(deviceId)
    }

    /**
     * Update device configuration.
     * Restarts monitoring with new configuration if device was being monitored.
     */
    suspend fun updateDevice(device: UpsDevice) {
        val wasMonitoring = deviceMonitors.containsKey(device.id)
        stopMonitoring(device.id)

        if (device.enabled && (wasMonitoring || isRunning)) {
            startMonitoring(device)
        }
    }

    /**
     * Get current monitoring state for a device.
     */
    fun getDeviceState(deviceId: String): DeviceMonitorState? {
        return deviceMonitors[deviceId]?.state
    }

    /**
     * Check if a device is currently being monitored.
     */
    fun isMonitoring(deviceId: String): Boolean {
        return deviceMonitors[deviceId]?.job?.isActive == true
    }

    private suspend fun loadAndStartMonitoring() {
        try {
            val devices = deviceRepository.findEnabled()
            logger.info("Found ${devices.size} enabled devices to monitor")

            devices.forEach { device ->
                startMonitoring(device)
            }
        } catch (e: Exception) {
            logger.error("Failed to load devices for monitoring", e)
        }
    }

    /**
     * Atomically start monitoring for a device if not already monitored.
     * Returns true if monitoring was started, false if already being monitored.
     */
    private fun startMonitoringIfAbsent(device: UpsDevice): Boolean {
        val scope = serviceScope ?: return false

        val state = DeviceMonitorState()
        val job = scope.launch {
            monitorDevice(device, state)
        }

        val newMonitor = DeviceMonitorJob(device, job, state)
        val existing = deviceMonitors.putIfAbsent(device.id, newMonitor)

        return if (existing == null) {
            logger.info("Starting monitoring for device ${device.id} (${device.name}) at ${device.host}:${device.port}")
            true
        } else {
            // Another thread already added this device, cancel our job
            job.cancel()
            logger.warn("Device ${device.id} is already being monitored")
            false
        }
    }

    /**
     * Start monitoring for a device (used during initial load where no race is possible).
     */
    private fun startMonitoring(device: UpsDevice) {
        startMonitoringIfAbsent(device)
    }

    private fun stopMonitoring(deviceId: String) {
        deviceMonitors.remove(deviceId)?.let { monitor ->
            logger.info("Stopping monitoring for device $deviceId")
            monitor.job.cancel()
            // Wait for job to complete to ensure clean shutdown
            runBlocking {
                withTimeoutOrNull(SHUTDOWN_TIMEOUT_MS) {
                    monitor.job.join()
                }
            }
        }
    }

    private suspend fun monitorDevice(device: UpsDevice, state: DeviceMonitorState) {
        logger.debug("Monitor coroutine started for device ${device.id}")

        while (currentCoroutineContext().isActive) {
            try {
                pollDevice(device, state)
            } catch (e: CancellationException) {
                logger.debug("Monitor coroutine cancelled for device ${device.id}")
                throw e
            } catch (e: Exception) {
                logger.error("Unexpected error in monitor loop for device ${device.id}", e)
            }

            delay(pollIntervalSeconds * 1000)
        }
    }

    private suspend fun pollDevice(device: UpsDevice, state: DeviceMonitorState) {
        try {
            val output = apcAccessClient.fetchStatusOutput(device.host, device.port)
            val status = ApcAccessParser.parse(output, device.id)

            if (isBlankStatus(status)) {
                logger.warn("Received blank status from device ${device.id}, treating as failure")
                handlePollFailure(device, state, ApcAccessException("Received blank/empty status"))
                return
            }

            // Store status in database
            statusRepository.insert(status)
            logger.debug("Stored status for device ${device.id}: ${status.status}")

            // Update state
            state.lastApcAccessOutput = output
            state.lastSuccessfulPoll = Clock.System.now()

            // Handle connection restoration
            if (state.consecutiveFailures.get() > 0) {
                handleConnectionRestored(device, state, status)
            }

            // Check for status changes (ONLINE <-> ON_BATTERY)
            checkStatusAndNotify(device, state, status, output)

            state.lastStatus = status.status.uppercase()
            state.consecutiveFailures.set(0)
            state.failureNotificationSent.set(false)

        } catch (e: ApcAccessException) {
            handlePollFailure(device, state, e)
        } catch (e: Exception) {
            logger.error("Unexpected error polling device ${device.id}", e)
            handlePollFailure(device, state, e)
        }
    }

    private suspend fun handlePollFailure(device: UpsDevice, state: DeviceMonitorState, error: Exception) {
        val failures = state.consecutiveFailures.incrementAndGet()
        logger.warn("Device ${device.id} poll failed ($failures/$failureThreshold): ${error.message}")

        if (failures >= failureThreshold && !state.failureNotificationSent.get()) {
            logger.error("Device ${device.id} reached $failureThreshold consecutive failures - sending notification")
            notificationDispatcher.sendConnectionLostAlert(device, failures, state.lastApcAccessOutput)
            state.failureNotificationSent.set(true)
            state.isConnectionLost.set(true)
        }
    }

    private suspend fun handleConnectionRestored(device: UpsDevice, state: DeviceMonitorState, status: UpsStatus) {
        val previousFailures = state.consecutiveFailures.get()
        logger.info("Device ${device.id} connection restored after $previousFailures failures")

        if (state.failureNotificationSent.get()) {
            notificationDispatcher.sendConnectionRestoredAlert(device, previousFailures, status)
        }

        state.isConnectionLost.set(false)
    }

    private suspend fun checkStatusAndNotify(
        device: UpsDevice,
        state: DeviceMonitorState,
        status: UpsStatus,
        apcAccessOutput: String
    ) {
        val currentStatus = status.status.uppercase()
        val previousStatus = state.lastStatus

        val isCurrentlyOffline = !currentStatus.contains("ONLINE")
        val wasOnlineOrUnknown = previousStatus == null || previousStatus.contains("ONLINE")

        if (isCurrentlyOffline && wasOnlineOrUnknown) {
            logger.warn("Device ${device.id} status changed to $currentStatus - sending alert")
            notificationDispatcher.sendStatusAlert(device, status, apcAccessOutput)
        } else if (!isCurrentlyOffline && previousStatus != null && !previousStatus.contains("ONLINE")) {
            logger.info("Device ${device.id} power restored: $previousStatus -> $currentStatus")
            notificationDispatcher.sendRecoveryAlert(device, status, previousStatus, apcAccessOutput)
        }
    }

    private fun isBlankStatus(status: UpsStatus): Boolean {
        return status.status.isBlank() ||
               status.apc.isBlank() ||
               status.hostname.isBlank() ||
               status.model.isBlank()
    }
}

/**
 * Holds the monitoring job and state for a single device.
 */
data class DeviceMonitorJob(
    val device: UpsDevice,
    val job: Job,
    val state: DeviceMonitorState
)

/**
 * Mutable state for a single device monitor.
 *
 * Uses atomic types for thread-safe access:
 * - AtomicInteger for consecutiveFailures (increment operations must be atomic)
 * - AtomicBoolean for flags that may be read/written from different threads
 * - @Volatile for reference types that are only assigned, not mutated
 *
 * Each device has its own state instance accessed by a single coroutine for writes,
 * but may be read by external threads via the API (e.g., getDeviceState()).
 */
class DeviceMonitorState {
    val consecutiveFailures = AtomicInteger(0)
    @Volatile var lastStatus: String? = null
    @Volatile var lastApcAccessOutput: String? = null
    @Volatile var lastSuccessfulPoll: Instant? = null
    val isConnectionLost = AtomicBoolean(false)
    val failureNotificationSent = AtomicBoolean(false)
}
