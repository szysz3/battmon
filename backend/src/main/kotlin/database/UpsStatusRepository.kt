package com.battmon.database

import com.battmon.model.UpsStatus
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction

class UpsStatusRepository {

    fun insert(status: UpsStatus): UpsStatus = transaction {
        val id = UpsStatusTable.insert {
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

    fun findLatest(): UpsStatus? = transaction {
        UpsStatusTable
            .selectAll()
            .orderBy(UpsStatusTable.timestamp, SortOrder.DESC)
            .limit(1)
            .map { it.toUpsStatus() }
            .firstOrNull()
    }

    fun findByTimeRange(
        from: Instant,
        to: Instant,
        limit: Int = 1000,
        offset: Long = 0
    ): List<UpsStatus> = transaction {
        UpsStatusTable
            .selectAll()
            .where { UpsStatusTable.timestamp greaterEq from and (UpsStatusTable.timestamp lessEq to) }
            .orderBy(UpsStatusTable.timestamp, SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .map { it.toUpsStatus() }
    }

    fun countByTimeRange(from: Instant, to: Instant): Long = transaction {
        UpsStatusTable
            .selectAll()
            .where { UpsStatusTable.timestamp greaterEq from and (UpsStatusTable.timestamp lessEq to) }
            .count()
    }

    fun deleteOlderThan(cutoff: Instant): Int = transaction {
        UpsStatusTable.deleteWhere { timestamp lessEq cutoff }
    }

    private fun ResultRow.toUpsStatus() = UpsStatus(
        id = this[UpsStatusTable.id],
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
