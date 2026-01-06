package com.battmon.config

data class DatabaseConfig(
    val jdbcUrl: String,
    val driver: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
)
