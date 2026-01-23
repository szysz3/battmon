package com.battmon.database

import com.battmon.model.UpsDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Repository for UPS device CRUD operations.
 */
class UpsDeviceRepository {

    /**
     * Insert a new UPS device.
     * @throws Exception if device with same id or host:port already exists
     */
    suspend fun insert(device: UpsDevice): UpsDevice = newSuspendedTransaction(Dispatchers.IO) {
        val now = Clock.System.now()
        UpsDeviceTable.insert {
            it[id] = device.id
            it[name] = device.name
            it[location] = device.location
            it[host] = device.host
            it[port] = device.port
            it[command] = device.command
            it[enabled] = device.enabled
            it[createdAt] = now
            it[updatedAt] = now
        }
        device.copy(createdAt = now, updatedAt = now)
    }

    /**
     * Update an existing UPS device.
     * @return Updated device or null if not found
     */
    suspend fun update(device: UpsDevice): UpsDevice? = newSuspendedTransaction(Dispatchers.IO) {
        val now = Clock.System.now()
        val updated = UpsDeviceTable.update({ UpsDeviceTable.id eq device.id }) {
            it[name] = device.name
            it[location] = device.location
            it[host] = device.host
            it[port] = device.port
            it[command] = device.command
            it[enabled] = device.enabled
            it[updatedAt] = now
        }
        if (updated > 0) {
            findByIdInternal(device.id)
        } else {
            null
        }
    }

    /**
     * Delete a UPS device by id.
     * Note: This will cascade delete all associated status records.
     * @return true if device was deleted, false if not found
     */
    suspend fun delete(id: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable.deleteWhere { UpsDeviceTable.id eq id } > 0
    }

    /**
     * Find a device by its id.
     */
    suspend fun findById(id: String): UpsDevice? = newSuspendedTransaction(Dispatchers.IO) {
        findByIdInternal(id)
    }

    /**
     * Find all devices.
     */
    suspend fun findAll(): List<UpsDevice> = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable
            .selectAll()
            .orderBy(UpsDeviceTable.name, SortOrder.ASC)
            .map { it.toUpsDevice() }
    }

    /**
     * Find all enabled devices (for monitoring).
     */
    suspend fun findEnabled(): List<UpsDevice> = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable
            .selectAll()
            .where { UpsDeviceTable.enabled eq true }
            .orderBy(UpsDeviceTable.name, SortOrder.ASC)
            .map { it.toUpsDevice() }
    }

    /**
     * Check if a device with the given id exists.
     */
    suspend fun exists(id: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable
            .selectAll()
            .where { UpsDeviceTable.id eq id }
            .count() > 0
    }

    /**
     * Check if a device with the given host:port combination already exists.
     * Used to prevent duplicate UPS registrations.
     *
     * @param excludeId Optional device id to exclude from check (for updates)
     */
    suspend fun existsByHostAndPort(host: String, port: Int, excludeId: String? = null): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val condition = (UpsDeviceTable.host eq host) and (UpsDeviceTable.port eq port)
            val finalCondition = if (excludeId != null) {
                condition and (UpsDeviceTable.id neq excludeId)
            } else {
                condition
            }
            UpsDeviceTable
                .selectAll()
                .where { finalCondition }
                .count() > 0
        }

    /**
     * Count total number of devices.
     */
    suspend fun count(): Long = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable.selectAll().count()
    }

    /**
     * Count enabled devices.
     */
    suspend fun countEnabled(): Long = newSuspendedTransaction(Dispatchers.IO) {
        UpsDeviceTable
            .selectAll()
            .where { UpsDeviceTable.enabled eq true }
            .count()
    }

    // Internal helper - must be called within a transaction
    private fun findByIdInternal(id: String): UpsDevice? {
        return UpsDeviceTable
            .selectAll()
            .where { UpsDeviceTable.id eq id }
            .map { it.toUpsDevice() }
            .firstOrNull()
    }

    private fun ResultRow.toUpsDevice() = UpsDevice(
        id = this[UpsDeviceTable.id],
        name = this[UpsDeviceTable.name],
        location = this[UpsDeviceTable.location],
        host = this[UpsDeviceTable.host],
        port = this[UpsDeviceTable.port],
        command = this[UpsDeviceTable.command],
        enabled = this[UpsDeviceTable.enabled],
        createdAt = this[UpsDeviceTable.createdAt],
        updatedAt = this[UpsDeviceTable.updatedAt]
    )
}
