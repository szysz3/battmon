package com.battmon.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UpsStatus(
    val id: Long? = null,
    val timestamp: Instant,

    val apc: String,
    val hostname: String,
    val version: String,
    val upsname: String,
    val cable: String,
    val driver: String,
    val upsmode: String,
    val model: String,
    val status: String,

    val starttime: String,
    val masterupd: String? = null,
    val master: String? = null,

    val linev: Double? = null,
    val loadpct: Double? = null,
    val bcharge: Double? = null,
    val timeleft: Double? = null,
    val battv: Double? = null,
    val nominv: Double? = null,
    val nombattv: Double? = null,
    val nompower: Double? = null,

    val lotrans: Double? = null,
    val hitrans: Double? = null,
    val sense: String? = null,

    val mbattchg: Int? = null,
    val mintimel: Int? = null,
    val maxtime: Int? = null,

    val lastxfer: String? = null,
    val numxfers: Int? = null,
    val tonbatt: Int? = null,
    val cumonbatt: Int? = null,
    val xoffbatt: String? = null,
    val selftest: String? = null,

    val statflag: String? = null,
    val serialno: String? = null,
    val battdate: String? = null,
    val date: String,
    val endApc: String
)
