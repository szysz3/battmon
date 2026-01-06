package com.battmon.config

data class UpsMonitorConfig(
    val pollIntervalSeconds: Long,
    val command: String
)
