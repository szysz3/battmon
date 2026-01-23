package com.battmon.config

data class UpsMonitorConfig(
    val pollIntervalSeconds: Long,
    /** Number of consecutive failures before sending connection lost notification */
    val failureThreshold: Int = DEFAULT_FAILURE_THRESHOLD
) {
    companion object {
        const val DEFAULT_FAILURE_THRESHOLD = 10
    }
}
