package com.battmon.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UpsStatus(
    val id: Long? = null,
    val timestamp: Instant,

    // Core identification
    val apc: String,
    val hostname: String,
    val version: String,
    val upsname: String,
    val cable: String,
    val driver: String,
    val upsmode: String,
    val model: String,
    val status: String,

    // Time information
    val starttime: String,
    val masterupd: String? = null,
    val master: String? = null,

    // Power metrics
    val linev: Double? = null,           // Line voltage in Volts
    val loadpct: Double? = null,         // Load percentage
    val bcharge: Double? = null,         // Battery charge percentage
    val timeleft: Double? = null,        // Time left in Minutes
    val battv: Double? = null,           // Battery voltage
    val nominv: Double? = null,          // Nominal input voltage
    val nombattv: Double? = null,        // Nominal battery voltage
    val nompower: Double? = null,        // Nominal power in Watts

    // Transfer thresholds
    val lotrans: Double? = null,         // Low transfer voltage
    val hitrans: Double? = null,         // High transfer voltage
    val sense: String? = null,           // Sensitivity

    // Battery settings
    val mbattchg: Int? = null,           // Min battery charge %
    val mintimel: Int? = null,           // Min time left minutes
    val maxtime: Int? = null,            // Max time seconds

    // Transfer/Event data
    val lastxfer: String? = null,        // Last transfer reason
    val numxfers: Int? = null,           // Number of transfers
    val tonbatt: Int? = null,            // Time on battery seconds
    val cumonbatt: Int? = null,          // Cumulative time on battery
    val xoffbatt: String? = null,        // Transfer off battery timestamp
    val selftest: String? = null,        // Self-test result

    // Other
    val statflag: String? = null,        // Status flag (hex)
    val serialno: String? = null,        // Serial number
    val battdate: String? = null,        // Battery date
    val date: String,                    // Reading date (from apcaccess)
    val endApc: String                   // End timestamp
)
