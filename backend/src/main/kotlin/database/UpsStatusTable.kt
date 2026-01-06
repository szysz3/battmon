package com.battmon.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UpsStatusTable : Table("ups_status") {
    val id = long("id").autoIncrement()
    val timestamp = timestamp("timestamp")

    // Core identification
    val apc = varchar("apc", 50)
    val hostname = varchar("hostname", 255)
    val version = varchar("version", 100)
    val upsname = varchar("upsname", 100)
    val cable = varchar("cable", 100)
    val driver = varchar("driver", 100)
    val upsmode = varchar("upsmode", 50)
    val model = varchar("model", 100)
    val status = varchar("status", 100)

    // Time information
    val starttime = varchar("starttime", 100)
    val masterupd = varchar("masterupd", 100).nullable()
    val master = varchar("master", 100).nullable()

    // Power metrics
    val linev = double("linev").nullable()
    val loadpct = double("loadpct").nullable()
    val bcharge = double("bcharge").nullable()
    val timeleft = double("timeleft").nullable()
    val battv = double("battv").nullable()
    val nominv = double("nominv").nullable()
    val nombattv = double("nombattv").nullable()
    val nompower = double("nompower").nullable()

    // Transfer thresholds
    val lotrans = double("lotrans").nullable()
    val hitrans = double("hitrans").nullable()
    val sense = varchar("sense", 50).nullable()

    // Battery settings
    val mbattchg = integer("mbattchg").nullable()
    val mintimel = integer("mintimel").nullable()
    val maxtime = integer("maxtime").nullable()

    // Transfer/Event data
    val lastxfer = varchar("lastxfer", 255).nullable()
    val numxfers = integer("numxfers").nullable()
    val tonbatt = integer("tonbatt").nullable()
    val cumonbatt = integer("cumonbatt").nullable()
    val xoffbatt = varchar("xoffbatt", 100).nullable()
    val selftest = varchar("selftest", 50).nullable()

    // Other
    val statflag = varchar("statflag", 50).nullable()
    val serialno = varchar("serialno", 100).nullable()
    val battdate = varchar("battdate", 50).nullable()
    val date = varchar("date", 100)
    val endApc = varchar("end_apc", 100)

    override val primaryKey = PrimaryKey(id)
}
