package com.battmon

import com.battmon.config.DatabaseConfig
import com.battmon.config.EmailConfig
import com.battmon.config.FirebaseConfig
import com.battmon.config.UpsMonitorConfig
import com.battmon.database.DatabaseFactory
import com.battmon.database.DeviceTokenRepository
import com.battmon.database.UpsStatusRepository
import com.battmon.plugins.configureSerialization
import com.battmon.routes.configureNotificationRoutes
import com.battmon.service.EmailNotificationService
import com.battmon.service.FcmNotificationService
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

    val firebaseConfig = FirebaseConfig(
        serviceAccountPath = environment.config.property("firebase.serviceAccountPath").getString(),
        enabled = environment.config.property("firebase.enabled").getString().toBoolean()
    )

    val emailConfig = EmailConfig(
        enabled = environment.config.property("email.enabled").getString().toBoolean(),
        smtpHost = environment.config.property("email.smtp.host").getString(),
        smtpPort = environment.config.property("email.smtp.port").getString().toInt(),
        smtpUsername = environment.config.property("email.smtp.username").getString(),
        smtpPassword = environment.config.property("email.smtp.password").getString(),
        smtpStartTls = environment.config.property("email.smtp.startTls").getString().toBoolean(),
        from = environment.config.property("email.from").getString(),
        to = environment.config.property("email.to").getString()
    )

    // Initialize database
    DatabaseFactory.init(
        jdbcUrl = dbConfig.jdbcUrl,
        driverClassName = dbConfig.driver,
        user = dbConfig.user,
        password = dbConfig.password,
        maxPoolSize = dbConfig.maxPoolSize
    )

    // Create repositories
    val repository = UpsStatusRepository()
    val deviceTokenRepository = DeviceTokenRepository()

    // Initialize Firebase
    val fcmService = FcmNotificationService(
        deviceTokenRepository = deviceTokenRepository,
        serviceAccountPath = firebaseConfig.serviceAccountPath,
        enabled = firebaseConfig.enabled
    )
    fcmService.initialize()

    // Initialize Email
    val emailService = EmailNotificationService(
        config = emailConfig
    )

    // Configure plugins
    configureSerialization()
    configureRouting(repository)
    configureNotificationRoutes(deviceTokenRepository, fcmService)

    // Start UPS monitor
    val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val monitorService = UpsMonitorService(
        repository = repository,
        pollIntervalSeconds = upsConfig.pollIntervalSeconds,
        apcAccessCommand = upsConfig.command,
        fcmService = fcmService,
        emailService = emailService
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
        DatabaseFactory.shutdown()
    }
}
