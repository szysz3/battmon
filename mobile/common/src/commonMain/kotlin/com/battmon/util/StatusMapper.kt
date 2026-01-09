package com.battmon.util

import androidx.compose.ui.graphics.Color
import com.battmon.ui.theme.*

enum class StatusCategory {
    ONLINE,
    ON_BATTERY,
    LOW_BATTERY,
    OFFLINE,
    WARNING
}

data class StatusInfo(
    val color: Color,
    val label: String,
    val category: StatusCategory
)

object StatusMapper {

    fun mapStatus(status: String): StatusInfo {
        val normalized = status.uppercase()
        return when {
            normalized.contains("ONLINE") -> StatusInfo(
                color = StatusOnline,
                label = "Online",
                category = StatusCategory.ONLINE
            )
            normalized.contains("ONBATT") -> StatusInfo(
                color = StatusOnBattery,
                label = "On Battery",
                category = StatusCategory.ON_BATTERY
            )
            normalized.contains("LOWBATT") -> StatusInfo(
                color = StatusOnBattery,
                label = "Low Battery",
                category = StatusCategory.LOW_BATTERY
            )
            normalized.contains("COMMLOST") -> StatusInfo(
                color = StatusOffline,
                label = "Comm Lost",
                category = StatusCategory.OFFLINE
            )
            else -> StatusInfo(
                color = StatusWarning,
                label = status,
                category = StatusCategory.WARNING
            )
        }
    }

    fun getAccentColor(status: String): Color {
        return mapStatus(status).color
    }

    @Suppress("unused")
    fun getLabel(status: String): String {
        return mapStatus(status).label
    }

    @Suppress("unused")
    fun getCategory(status: String): StatusCategory {
        return mapStatus(status).category
    }
}
