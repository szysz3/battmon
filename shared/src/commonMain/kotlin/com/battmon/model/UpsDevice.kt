package com.battmon.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a UPS device configuration for monitoring.
 *
 * @property id User-defined unique identifier (e.g., "office-ups-1"). Must match pattern: ^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$
 * @property name Display name (e.g., "Office UPS")
 * @property location Physical location (e.g., "Server Room"), optional
 * @property host IP address or hostname for apcaccess NIS connection
 * @property port Network port for apcaccess NIS (default 3551)
 * @property command Command to execute (default "apcaccess status")
 * @property enabled Whether monitoring is enabled for this device
 * @property createdAt Record creation timestamp
 * @property updatedAt Last modification timestamp
 */
@Serializable
data class UpsDevice(
    val id: String,
    val name: String,
    val location: String? = null,
    val host: String,
    val port: Int = 3551,
    val command: String = "apcaccess status",
    val enabled: Boolean = true,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
) {
    companion object {
        /**
         * Regex pattern for valid device IDs:
         * - 3-50 characters
         * - Lowercase letters, numbers, and hyphens only
         * - Must start and end with letter or number
         */
        val ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$")

        const val DEFAULT_PORT = 3551
        const val DEFAULT_COMMAND = "apcaccess status"

        fun isValidId(id: String): Boolean = ID_PATTERN.matches(id)
    }
}

/**
 * Request DTO for creating a new UPS device.
 */
@Serializable
data class CreateDeviceRequest(
    val id: String,
    val name: String,
    val location: String? = null,
    val host: String,
    val port: Int = UpsDevice.DEFAULT_PORT,
    val enabled: Boolean = true
)

/**
 * Request DTO for updating an existing UPS device.
 * Note: id cannot be changed after creation.
 */
@Serializable
data class UpdateDeviceRequest(
    val name: String,
    val location: String? = null,
    val host: String,
    val port: Int = UpsDevice.DEFAULT_PORT,
    val enabled: Boolean = true
)

/**
 * Request DTO for testing device connectivity.
 */
@Serializable
data class TestConnectionRequest(
    val host: String,
    val port: Int = UpsDevice.DEFAULT_PORT
)

/**
 * Response DTO for connection test results.
 */
@Serializable
data class TestConnectionResponse(
    val success: Boolean,
    val message: String,
    val status: UpsStatus? = null
)

/**
 * Response DTO combining device configuration with its latest status.
 * Used for the device list endpoint.
 */
@Serializable
data class DeviceWithStatus(
    val device: UpsDevice,
    val status: UpsStatus? = null,
    val connectionHealth: ConnectionHealth = ConnectionHealth.UNKNOWN
)

/**
 * Represents the connection health state of a device.
 */
@Serializable
enum class ConnectionHealth {
    /** Device is reachable and responding */
    HEALTHY,
    /** Device has intermittent connection issues */
    DEGRADED,
    /** Device is unreachable */
    OFFLINE,
    /** Connection status is unknown (never polled) */
    UNKNOWN
}
