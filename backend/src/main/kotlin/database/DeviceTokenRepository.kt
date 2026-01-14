package com.battmon.database

import com.battmon.model.DeviceToken
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction

class DeviceTokenRepository {

    fun upsert(deviceToken: DeviceToken): DeviceToken = transaction {
        val now = Clock.System.now()

        // Use PostgreSQL's INSERT ... ON CONFLICT DO UPDATE to handle concurrent inserts atomically
        // This prevents race conditions when multiple clients register simultaneously
        // On conflict: update deviceName, platform, updatedAt, lastSeen but preserve original createdAt
        DeviceTokenTable.upsert(
            keys = arrayOf(DeviceTokenTable.fcmToken),
            onUpdate = {
                it[DeviceTokenTable.deviceName] = deviceToken.deviceName
                it[DeviceTokenTable.platform] = deviceToken.platform
                it[DeviceTokenTable.updatedAt] = now
                it[DeviceTokenTable.lastSeen] = now
                // Note: createdAt is intentionally omitted to preserve the original creation timestamp
            }
        ) {
            it[fcmToken] = deviceToken.fcmToken
            it[this.deviceName] = deviceToken.deviceName
            it[platform] = deviceToken.platform
            it[createdAt] = now
            it[updatedAt] = now
            it[lastSeen] = now
        }

        // Fetch the result to return the complete token with database-generated ID
        DeviceTokenTable
            .selectAll()
            .where { DeviceTokenTable.fcmToken eq deviceToken.fcmToken }
            .single()
            .toDeviceToken()
    }

    fun findAll(): List<DeviceToken> = transaction {
        DeviceTokenTable
            .selectAll()
            .orderBy(DeviceTokenTable.createdAt, SortOrder.DESC)
            .map { it.toDeviceToken() }
    }

    fun findByToken(fcmToken: String): DeviceToken? = transaction {
        DeviceTokenTable
            .selectAll()
            .where { DeviceTokenTable.fcmToken eq fcmToken }
            .map { it.toDeviceToken() }
            .singleOrNull()
    }

    fun delete(fcmToken: String): Boolean = transaction {
        DeviceTokenTable.deleteWhere { DeviceTokenTable.fcmToken eq fcmToken } > 0
    }

    fun updateLastSeen(fcmToken: String): Boolean = transaction {
        DeviceTokenTable.update({ DeviceTokenTable.fcmToken eq fcmToken }) {
            it[lastSeen] = Clock.System.now()
        } > 0
    }

    fun deleteInactive(cutoff: Instant): Int = transaction {
        DeviceTokenTable.deleteWhere { lastSeen less cutoff }
    }

    private fun ResultRow.toDeviceToken() = DeviceToken(
        id = this[DeviceTokenTable.id],
        fcmToken = this[DeviceTokenTable.fcmToken],
        deviceName = this[DeviceTokenTable.deviceName],
        platform = this[DeviceTokenTable.platform],
        createdAt = this[DeviceTokenTable.createdAt],
        updatedAt = this[DeviceTokenTable.updatedAt],
        lastSeen = this[DeviceTokenTable.lastSeen]
    )
}
