package com.battmon.config

data class EmailConfig(
    val enabled: Boolean,
    val smtpHost: String,
    val smtpPort: Int,
    val smtpUsername: String,
    val smtpPassword: String,
    val smtpStartTls: Boolean,
    val from: String,
    val to: String
)
