package com.battmon

import com.battmon.config.DatabaseConfig
import com.battmon.config.UpsMonitorConfig
import com.battmon.database.DatabaseFactory
import com.battmon.database.UpsStatusRepository
import com.battmon.plugins.configureSerialization
import com.battmon.service.RetentionService
import com.battmon.service.UpsMonitorService
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Load configuration
    val dbConfig = DatabaseConfig(
        jdbcUrl = environment.config.property("database.jdbcUrl").getString(),
        driver = environment.config.property("database.driver").getString(),
        user = environment.config.property("database.user").getString(),
        password = environment.config.property("database.password").getString(),
        maxPoolSize = environment.config.property("database.maxPoolSize").getString().toInt()
    )

    val upsConfig = UpsMonitorConfig(
        pollIntervalSeconds = environment.config.property("ups.monitor.pollIntervalSeconds").getString().toLong(),
        command = environment.config.property("ups.monitor.command").getString()
    )
    val retentionDays = environment.config.property("database.retentionDays").getString().toInt()
    val retentionIntervalHours = environment.config.property("database.retentionIntervalHours").getString().toLong()

    // Initialize database
    DatabaseFactory.init(
        jdbcUrl = dbConfig.jdbcUrl,
        driverClassName = dbConfig.driver,
        user = dbConfig.user,
        password = dbConfig.password,
        maxPoolSize = dbConfig.maxPoolSize
    )

    // Create repository
    val repository = UpsStatusRepository()

    // Configure plugins
    configureSerialization()
    configureRouting(repository)

    // Start UPS monitor
    val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val monitorService = UpsMonitorService(
        repository = repository,
        pollIntervalSeconds = upsConfig.pollIntervalSeconds,
        apcAccessCommand = upsConfig.command
    )
    val retentionService = RetentionService(
        repository = repository,
        retentionDays = retentionDays,
        cleanupIntervalHours = retentionIntervalHours
    )
    monitorService.start(monitorScope)
    retentionService.start(monitorScope)

    // Cleanup on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        monitorService.stop()
        retentionService.stop()
    }
}
