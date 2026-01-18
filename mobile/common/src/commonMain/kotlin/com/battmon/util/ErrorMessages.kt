package com.battmon.util

object ErrorMessages {
    const val FAILED_TO_LOAD_STATUS = "Failed to load UPS status. Please check your connection."
    const val FAILED_TO_LOAD_HISTORY = "Failed to load history. Please check your connection."
    const val UNKNOWN_ERROR = "An unexpected error occurred. Please try again."

    fun withDetail(baseMessage: String, detail: String?): String {
        return if (detail.isNullOrBlank()) {
            baseMessage
        } else {
            "$baseMessage\nDetails: $detail"
        }
    }
}
