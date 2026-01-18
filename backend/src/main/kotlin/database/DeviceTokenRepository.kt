package com.battmon.database

import com.battmon.model.DeviceToken
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class DeviceTokenRepository {

    suspend fun upsert(deviceToken: DeviceToken): DeviceToken = newSuspendedTransaction(Dispatchers.IO) {
        val now = Clock.System.now()

        DeviceTokenTable.upsert(
            keys = arrayOf(DeviceTokenTable.fcmToken),
            onUpdate = {
                it[DeviceTokenTable.deviceName] = deviceToken.deviceName
                it[DeviceTokenTable.platform] = deviceToken.platform
                it[DeviceTokenTable.updatedAt] = now
                it[DeviceTokenTable.lastSeen] = now
            }
        ) {
            it[fcmToken] = deviceToken.fcmToken
            it[this.deviceName] = deviceToken.deviceName
            it[platform] = deviceToken.platform
            it[createdAt] = now
            it[updatedAt] = now
            it[lastSeen] = now
        }

        DeviceTokenTable
            .selectAll()
            .where { DeviceTokenTable.fcmToken eq deviceToken.fcmToken }
            .single()
            .toDeviceToken()
    }

    suspend fun findAll(): List<DeviceToken> = newSuspendedTransaction(Dispatchers.IO) {
        DeviceTokenTable
            .selectAll()
            .orderBy(DeviceTokenTable.createdAt, SortOrder.DESC)
            .map { it.toDeviceToken() }
    }

    suspend fun findByToken(fcmToken: String): DeviceToken? = newSuspendedTransaction(Dispatchers.IO) {
        DeviceTokenTable
            .selectAll()
            .where { DeviceTokenTable.fcmToken eq fcmToken }
            .map { it.toDeviceToken() }
            .singleOrNull()
    }

    suspend fun delete(fcmToken: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        DeviceTokenTable.deleteWhere { DeviceTokenTable.fcmToken eq fcmToken } > 0
    }

    suspend fun updateLastSeen(fcmToken: String): Boolean = newSuspendedTransaction(Dispatchers.IO) {
        DeviceTokenTable.update({ DeviceTokenTable.fcmToken eq fcmToken }) {
            it[lastSeen] = Clock.System.now()
        } > 0
    }

    suspend fun deleteInactive(cutoff: Instant): Int = newSuspendedTransaction(Dispatchers.IO) {
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
