package com.battmon

enum class StatusFilter {
    ALL,
    ONLINE,
    OFFLINE_OR_ON_BATTERY;

    companion object {
        fun fromQuery(value: String?): StatusFilter? = when (value?.trim()?.lowercase()) {
            null, "", "all" -> ALL
            "online" -> ONLINE
            "offline_or_on_battery", "offline", "on_battery" -> OFFLINE_OR_ON_BATTERY
            else -> null
        }
    }
}
