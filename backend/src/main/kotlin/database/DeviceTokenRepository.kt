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

        // Try to find existing token
        val existing = DeviceTokenTable
            .selectAll()
            .where { DeviceTokenTable.fcmToken eq deviceToken.fcmToken }
            .singleOrNull()

        if (existing != null) {
            // Update existing token
            DeviceTokenTable.update({ DeviceTokenTable.fcmToken eq deviceToken.fcmToken }) {
                it[deviceName] = deviceToken.deviceName
                it[platform] = deviceToken.platform
                it[updatedAt] = now
                it[lastSeen] = now
            }
            deviceToken.copy(
                id = existing[DeviceTokenTable.id],
                createdAt = existing[DeviceTokenTable.createdAt],
                updatedAt = now,
                lastSeen = now
            )
        } else {
            // Insert new token
            val id = DeviceTokenTable.insert {
                it[fcmToken] = deviceToken.fcmToken
                it[deviceName] = deviceToken.deviceName
                it[platform] = deviceToken.platform
                it[createdAt] = now
                it[updatedAt] = now
                it[lastSeen] = now
            }[DeviceTokenTable.id]

            deviceToken.copy(
                id = id,
                createdAt = now,
                updatedAt = now,
                lastSeen = now
            )
        }
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
