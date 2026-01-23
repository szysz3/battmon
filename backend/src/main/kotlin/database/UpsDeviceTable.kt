package com.battmon.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Database table definition for UPS device configurations.
 *
 * Stores device connection info and metadata for multi-UPS monitoring.
 */
object UpsDeviceTable : Table("ups_devices") {
    /** User-defined unique identifier (e.g., "office-ups-1") */
    val id = varchar("id", 255)

    /** Display name for the device */
    val name = varchar("name", 255)

    /** Physical location description (optional) */
    val location = varchar("location", 255).nullable()

    /** IP address or hostname for apcaccess NIS connection */
    val host = varchar("host", 255)

    /** Network port for apcaccess NIS (default 3551) */
    val port = integer("port").default(3551)

    /** Command to execute (for future flexibility) */
    val command = text("command").default("apcaccess status")

    /** Whether monitoring is enabled for this device */
    val enabled = bool("enabled").default(true)

    /** Record creation timestamp */
    val createdAt = timestamp("created_at")

    /** Last modification timestamp */
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
