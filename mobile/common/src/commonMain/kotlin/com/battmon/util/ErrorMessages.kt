package com.battmon.util

/**
 * Centralized error messages for the application
 *
 * Benefits:
 * - Consistent error messages across the app
 * - Easy to update messages in one place
 * - Preparation for future localization
 */
object ErrorMessages {
    // Network errors
    const val FAILED_TO_LOAD_STATUS = "Failed to load UPS status. Please check your connection."
    const val FAILED_TO_LOAD_HISTORY = "Failed to load history. Please check your connection."
    const val NETWORK_TIMEOUT = "Request timed out. Please try again."
    const val NO_INTERNET = "No internet connection. Please check your network."

    // Generic errors
    const val UNKNOWN_ERROR = "An unexpected error occurred. Please try again."
    const val SERVER_ERROR = "Server error. Please try again later."

    // Data errors
    const val NO_DATA_AVAILABLE = "No data available for the selected time range."
    const val INVALID_DATA = "Received invalid data from server."

    /**
     * Format an error message with additional context
     */
    fun withDetail(baseMessage: String, detail: String?): String {
        return if (detail.isNullOrBlank()) {
            baseMessage
        } else {
            "$baseMessage\nDetails: $detail"
        }
    }
}
