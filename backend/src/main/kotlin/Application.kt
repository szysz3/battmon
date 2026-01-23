package com.battmon

import com.battmon.config.DatabaseConfig
import com.battmon.config.EmailConfig
import com.battmon.config.FirebaseConfig
import com.battmon.config.UpsMonitorConfig
import com.battmon.database.DatabaseFactory
import com.battmon.database.DeviceTokenRepository
import com.battmon.database.UpsDeviceRepository
import com.battmon.database.UpsStatusRepository
import com.battmon.plugins.configureSerialization
import com.battmon.routes.configureDeviceRoutes
import com.battmon.routes.configureNotificationRoutes
import com.battmon.service.EmailNotificationService
import com.battmon.service.FcmNotificationService
import com.battmon.service.HttpApcAccessClient
import com.battmon.service.DefaultMultiDeviceNotificationDispatcher
import com.battmon.service.RetentionService
import com.battmon.service.MultiUpsMonitorService
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val dbConfig = DatabaseConfig(
        jdbcUrl = environment.config.property("database.jdbcUrl").getString(),
        driver = environment.config.property("database.driver").getString(),
        user = environment.config.property("database.user").getString(),
        password = environment.config.property("database.password").getString(),
        maxPoolSize = environment.config.property("database.maxPoolSize").getString().toInt()
    )

    val upsConfig = UpsMonitorConfig(
        pollIntervalSeconds = environment.config.property("ups.monitor.pollIntervalSeconds").getString().toLong(),
        failureThreshold = environment.config.property("ups.monitor.failureThreshold").getString().toInt()
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

    DatabaseFactory.init(
        jdbcUrl = dbConfig.jdbcUrl,
        driverClassName = dbConfig.driver,
        user = dbConfig.user,
        password = dbConfig.password,
        maxPoolSize = dbConfig.maxPoolSize
    )

    val statusRepository = UpsStatusRepository()
    val deviceRepository = UpsDeviceRepository()
    val deviceTokenRepository = DeviceTokenRepository()

    val fcmService = FcmNotificationService(
        deviceTokenRepository = deviceTokenRepository,
        serviceAccountPath = firebaseConfig.serviceAccountPath,
        enabled = firebaseConfig.enabled
    )
    fcmService.initialize()

    val emailService = EmailNotificationService(
        config = emailConfig
    )

    val apcAccessProxyUrl = environment.config.propertyOrNull("ups.apcaccess.proxyUrl")
        ?.getString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException(
            "ups.apcaccess.proxyUrl (UPS_APCACCESS_PROXY_URL) must be set; " +
                "apcaccess is required to run on the host via the proxy"
        )

    val apcAccessClient = HttpApcAccessClient(apcAccessProxyUrl)

    val logger = LoggerFactory.getLogger("Application")
    logger.info("Using apcaccess proxy at {}", apcAccessProxyUrl)
    val supervisorJob = SupervisorJob()
    val monitorScope = CoroutineScope(Dispatchers.Default + supervisorJob)

    // Multi-device monitor service
    val monitorService = MultiUpsMonitorService(
        deviceRepository = deviceRepository,
        statusRepository = statusRepository,
        apcAccessClient = apcAccessClient,
        notificationDispatcher = DefaultMultiDeviceNotificationDispatcher(
            fcmService = fcmService,
            emailService = emailService
        ),
        pollIntervalSeconds = upsConfig.pollIntervalSeconds,
        failureThreshold = upsConfig.failureThreshold
    )

    val retentionService = RetentionService(
        repository = statusRepository,
        retentionDays = retentionDays,
        cleanupIntervalHours = retentionIntervalHours
    )

    configureSerialization()
    configureRouting(statusRepository)
    configureDeviceRoutes(deviceRepository, statusRepository, apcAccessClient, monitorService)
    configureNotificationRoutes(deviceTokenRepository, fcmService)

    monitorService.start(monitorScope)
    retentionService.start(monitorScope)

    environment.monitor.subscribe(ApplicationStopped) {
        logger.info("Application stopping, initiating graceful shutdown...")

        monitorService.stop()
        retentionService.stop()

        monitorScope.cancel("Application shutting down")

        runBlocking {
            delay(500)
        }

        DatabaseFactory.shutdown()
        logger.info("Shutdown complete")
    }
}
