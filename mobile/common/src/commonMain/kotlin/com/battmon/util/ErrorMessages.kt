package com.battmon.util

@Suppress("unused")
object ErrorMessages {
    const val FAILED_TO_LOAD_STATUS = "Failed to load UPS status. Please check your connection."
    const val FAILED_TO_LOAD_HISTORY = "Failed to load history. Please check your connection."
    const val NETWORK_TIMEOUT = "Request timed out. Please try again."
    const val NO_INTERNET = "No internet connection. Please check your network."

    const val UNKNOWN_ERROR = "An unexpected error occurred. Please try again."
    const val SERVER_ERROR = "Server error. Please try again later."

    const val NO_DATA_AVAILABLE = "No data available for the selected time range."
    const val INVALID_DATA = "Received invalid data from server."

    fun withDetail(baseMessage: String, detail: String?): String {
        return if (detail.isNullOrBlank()) {
            baseMessage
        } else {
            "$baseMessage\nDetails: $detail"
        }
    }
}
