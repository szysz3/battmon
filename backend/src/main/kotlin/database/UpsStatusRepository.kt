package com.battmon.database

import com.battmon.model.UpsStatus
import kotlinx.datetime.Instant
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notLike
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import com.battmon.StatusFilter
import org.jetbrains.exposed.sql.LowerCase

class UpsStatusRepository {

    suspend fun insert(status: UpsStatus): UpsStatus = newSuspendedTransaction(Dispatchers.IO) {
        val id = UpsStatusTable.insert {
            it[upsDeviceId] = status.upsDeviceId
            it[timestamp] = status.timestamp
            it[apc] = status.apc
            it[hostname] = status.hostname
            it[version] = status.version
            it[upsname] = status.upsname
            it[cable] = status.cable
            it[driver] = status.driver
            it[upsmode] = status.upsmode
            it[model] = status.model
            it[UpsStatusTable.status] = status.status
            it[starttime] = status.starttime
            it[masterupd] = status.masterupd
            it[master] = status.master
            it[linev] = status.linev
            it[loadpct] = status.loadpct
            it[bcharge] = status.bcharge
            it[timeleft] = status.timeleft
            it[battv] = status.battv
            it[nominv] = status.nominv
            it[nombattv] = status.nombattv
            it[nompower] = status.nompower
            it[lotrans] = status.lotrans
            it[hitrans] = status.hitrans
            it[sense] = status.sense
            it[mbattchg] = status.mbattchg
            it[mintimel] = status.mintimel
            it[maxtime] = status.maxtime
            it[lastxfer] = status.lastxfer
            it[numxfers] = status.numxfers
            it[tonbatt] = status.tonbatt
            it[cumonbatt] = status.cumonbatt
            it[xoffbatt] = status.xoffbatt
            it[selftest] = status.selftest
            it[statflag] = status.statflag
            it[serialno] = status.serialno
            it[battdate] = status.battdate
            it[date] = status.date
            it[endApc] = status.endApc
        }[UpsStatusTable.id]

        status.copy(id = id)
    }

    /**
     * Find the latest status record (across all devices).
     * For backward compatibility with single-device mode.
     */
    suspend fun findLatest(): UpsStatus? = newSuspendedTransaction(Dispatchers.IO) {
        UpsStatusTable
            .selectAll()
            .orderBy(UpsStatusTable.timestamp, SortOrder.DESC)
            .limit(1)
            .map { it.toUpsStatus() }
            .firstOrNull()
    }

    /**
     * Find the latest status for a specific device.
     */
    suspend fun findLatestByDevice(deviceId: String): UpsStatus? = newSuspendedTransaction(Dispatchers.IO) {
        UpsStatusTable
            .selectAll()
            .where { UpsStatusTable.upsDeviceId eq deviceId }
            .orderBy(UpsStatusTable.timestamp, SortOrder.DESC)
            .limit(1)
            .map { it.toUpsStatus() }
            .firstOrNull()
    }

    /**
     * Find the latest status for each device.
     * Returns a map of deviceId to latest status.
     *
     * Uses a single query with a subquery to find max timestamp per device,
     * avoiding N+1 query problem.
     */
    suspend fun findLatestForAllDevices(): Map<String, UpsStatus> = newSuspendedTransaction(Dispatchers.IO) {
        // Subquery to get max timestamp per device
        val maxTimestamp = UpsStatusTable.timestamp.max()
        val maxTimestampPerDevice = UpsStatusTable
            .select(UpsStatusTable.upsDeviceId, maxTimestamp)
            .where { UpsStatusTable.upsDeviceId.isNotNull() }
            .groupBy(UpsStatusTable.upsDeviceId)
            .alias("latest")

        val latestDeviceId = maxTimestampPerDevice[UpsStatusTable.upsDeviceId]
        val latestTimestamp = maxTimestampPerDevice[maxTimestamp]

        // Join with the alias to get full records for latest timestamps
        UpsStatusTable
            .join(maxTimestampPerDevice, JoinType.INNER) {
                (UpsStatusTable.upsDeviceId eq latestDeviceId) and
                    (UpsStatusTable.timestamp eq latestTimestamp)
            }
            .selectAll()
            .map { it.toUpsStatus() }
            .mapNotNull { status -> status.upsDeviceId?.let { it to status } }
            .toMap()
    }

    /**
     * Find status records within a time range.
     *
     * @param from Start of time range (inclusive)
     * @param to End of time range (inclusive)
     * @param deviceId Optional device filter (null = all devices)
     * @param limit Maximum records to return
     * @param offset Number of records to skip
     * @param statusFilter Filter by status type
     */
    suspend fun findByTimeRange(
        from: Instant,
        to: Instant,
        deviceId: String? = null,
        limit: Int = 1000,
        offset: Long = 0,
        statusFilter: StatusFilter = StatusFilter.ALL
    ): List<UpsStatus> = newSuspendedTransaction(Dispatchers.IO) {
        val timeCondition = UpsStatusTable.timestamp greaterEq from and (UpsStatusTable.timestamp lessEq to)
        val deviceCondition = if (deviceId != null) {
            UpsStatusTable.upsDeviceId eq deviceId
        } else {
            Op.TRUE
        }
        val statusCondition = when (statusFilter) {
            StatusFilter.ALL -> Op.TRUE
            StatusFilter.ONLINE -> LowerCase(UpsStatusTable.status) like "%online%"
            StatusFilter.OFFLINE_OR_ON_BATTERY -> LowerCase(UpsStatusTable.status) notLike "%online%"
        }
        UpsStatusTable
            .selectAll()
            .where { timeCondition and deviceCondition and statusCondition }
            .orderBy(UpsStatusTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toUpsStatus() }
    }

    /**
     * Count status records within a time range.
     *
     * @param from Start of time range (inclusive)
     * @param to End of time range (inclusive)
     * @param deviceId Optional device filter (null = all devices)
     * @param statusFilter Filter by status type
     */
    suspend fun countByTimeRange(
        from: Instant,
        to: Instant,
        deviceId: String? = null,
        statusFilter: StatusFilter = StatusFilter.ALL
    ): Long = newSuspendedTransaction(Dispatchers.IO) {
        val timeCondition = UpsStatusTable.timestamp greaterEq from and (UpsStatusTable.timestamp lessEq to)
        val deviceCondition = if (deviceId != null) {
            UpsStatusTable.upsDeviceId eq deviceId
        } else {
            Op.TRUE
        }
        val statusCondition = when (statusFilter) {
            StatusFilter.ALL -> Op.TRUE
            StatusFilter.ONLINE -> LowerCase(UpsStatusTable.status) like "%online%"
            StatusFilter.OFFLINE_OR_ON_BATTERY -> LowerCase(UpsStatusTable.status) notLike "%online%"
        }
        UpsStatusTable
            .selectAll()
            .where { timeCondition and deviceCondition and statusCondition }
            .count()
    }

    suspend fun deleteOlderThan(cutoff: Instant): Int = newSuspendedTransaction(Dispatchers.IO) {
        UpsStatusTable.deleteWhere { timestamp lessEq cutoff }
    }

    private fun ResultRow.toUpsStatus() = UpsStatus(
        id = this[UpsStatusTable.id],
        upsDeviceId = this[UpsStatusTable.upsDeviceId],
        timestamp = this[UpsStatusTable.timestamp],
        apc = this[UpsStatusTable.apc],
        hostname = this[UpsStatusTable.hostname],
        version = this[UpsStatusTable.version],
        upsname = this[UpsStatusTable.upsname],
        cable = this[UpsStatusTable.cable],
        driver = this[UpsStatusTable.driver],
        upsmode = this[UpsStatusTable.upsmode],
        model = this[UpsStatusTable.model],
        status = this[UpsStatusTable.status],
        starttime = this[UpsStatusTable.starttime],
        masterupd = this[UpsStatusTable.masterupd],
        master = this[UpsStatusTable.master],
        linev = this[UpsStatusTable.linev],
        loadpct = this[UpsStatusTable.loadpct],
        bcharge = this[UpsStatusTable.bcharge],
        timeleft = this[UpsStatusTable.timeleft],
        battv = this[UpsStatusTable.battv],
        nominv = this[UpsStatusTable.nominv],
        nombattv = this[UpsStatusTable.nombattv],
        nompower = this[UpsStatusTable.nompower],
        lotrans = this[UpsStatusTable.lotrans],
        hitrans = this[UpsStatusTable.hitrans],
        sense = this[UpsStatusTable.sense],
        mbattchg = this[UpsStatusTable.mbattchg],
        mintimel = this[UpsStatusTable.mintimel],
        maxtime = this[UpsStatusTable.maxtime],
        lastxfer = this[UpsStatusTable.lastxfer],
        numxfers = this[UpsStatusTable.numxfers],
        tonbatt = this[UpsStatusTable.tonbatt],
        cumonbatt = this[UpsStatusTable.cumonbatt],
        xoffbatt = this[UpsStatusTable.xoffbatt],
        selftest = this[UpsStatusTable.selftest],
        statflag = this[UpsStatusTable.statflag],
        serialno = this[UpsStatusTable.serialno],
        battdate = this[UpsStatusTable.battdate],
        date = this[UpsStatusTable.date],
        endApc = this[UpsStatusTable.endApc]
    )
}
