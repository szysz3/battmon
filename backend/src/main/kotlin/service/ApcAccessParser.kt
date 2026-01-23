package com.battmon.service

import com.battmon.model.UpsStatus
import kotlinx.datetime.Clock

object ApcAccessParser {
    private val FIELD_REGEX = Regex("""^([A-Z ]+)\s*:\s*(.*)$""")

    /**
     * Parse apcaccess output into UpsStatus.
     *
     * @param output Raw apcaccess command output
     * @param deviceId Optional device ID to associate with this status
     * @return Parsed UpsStatus object
     */
    fun parse(output: String, deviceId: String? = null): UpsStatus {
        val fields = mutableMapOf<String, String>()

        output.lines().forEach { line ->
            val match = FIELD_REGEX.find(line.trim())
            if (match != null) {
                val (key, value) = match.destructured
                fields[key.trim()] = value.trim()
            }
        }

        return UpsStatus(
            upsDeviceId = deviceId,
            timestamp = Clock.System.now(),
            apc = fields["APC"] ?: "",
            hostname = fields["HOSTNAME"] ?: "",
            version = fields["VERSION"] ?: "",
            upsname = fields["UPSNAME"] ?: "",
            cable = fields["CABLE"] ?: "",
            driver = fields["DRIVER"] ?: "",
            upsmode = fields["UPSMODE"] ?: "",
            model = fields["MODEL"] ?: "",
            status = fields["STATUS"] ?: "",
            starttime = fields["STARTTIME"] ?: "",
            masterupd = fields["MASTERUPD"],
            master = fields["MASTER"],
            linev = fields["LINEV"]?.extractDouble(),
            loadpct = fields["LOADPCT"]?.extractDouble(),
            bcharge = fields["BCHARGE"]?.extractDouble(),
            timeleft = fields["TIMELEFT"]?.extractDouble(),
            battv = fields["BATTV"]?.extractDouble(),
            nominv = fields["NOMINV"]?.extractDouble(),
            nombattv = fields["NOMBATTV"]?.extractDouble(),
            nompower = fields["NOMPOWER"]?.extractDouble(),
            lotrans = fields["LOTRANS"]?.extractDouble(),
            hitrans = fields["HITRANS"]?.extractDouble(),
            sense = fields["SENSE"],
            mbattchg = fields["MBATTCHG"]?.extractInt(),
            mintimel = fields["MINTIMEL"]?.extractInt(),
            maxtime = fields["MAXTIME"]?.extractInt(),
            lastxfer = fields["LASTXFER"],
            numxfers = fields["NUMXFERS"]?.extractInt(),
            tonbatt = fields["TONBATT"]?.extractInt(),
            cumonbatt = fields["CUMONBATT"]?.extractInt(),
            xoffbatt = fields["XOFFBATT"],
            selftest = fields["SELFTEST"],
            statflag = fields["STATFLAG"],
            serialno = fields["SERIALNO"],
            battdate = fields["BATTDATE"],
            date = fields["DATE"] ?: "",
            endApc = fields["END APC"] ?: ""
        )
    }

    private fun String.extractDouble(): Double? =
        this.split(" ").firstOrNull()?.toDoubleOrNull()

    private fun String.extractInt(): Int? =
        this.split(" ").firstOrNull()?.toIntOrNull()
}
