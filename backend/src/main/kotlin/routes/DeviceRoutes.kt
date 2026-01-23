package com.battmon.routes

import com.battmon.database.UpsDeviceRepository
import com.battmon.database.UpsStatusRepository
import com.battmon.model.*
import com.battmon.service.ApcAccessClient
import com.battmon.service.ApcAccessParser
import com.battmon.service.MultiUpsMonitorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DeviceRoutes")

/**
 * Configure device management routes.
 */
fun Application.configureDeviceRoutes(
    deviceRepository: UpsDeviceRepository,
    statusRepository: UpsStatusRepository,
    apcAccessClient: ApcAccessClient,
    monitorService: MultiUpsMonitorService
) {
    routing {
        route("/devices") {
            // GET /devices - List all devices
            get {
                val devices = deviceRepository.findAll()
                call.respond(devices)
            }

            // GET /devices/{id} - Get device by ID
            get("/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Device ID is required")
                )

                val device = deviceRepository.findById(id)
                if (device != null) {
                    call.respond(device)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Device not found: $id")
                    )
                }
            }

            // POST /devices - Create new device
            post {
                val request = try {
                    call.receive<CreateDeviceRequest>()
                } catch (e: Exception) {
                    logger.warn("Invalid create device request: ${e.message}")
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request body: ${e.message}")
                    )
                }

                // Validate device ID format
                if (!UpsDevice.isValidId(request.id)) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "error" to "Invalid device ID format",
                            "details" to "ID must be 3-50 characters, lowercase letters, numbers, and hyphens only, starting and ending with letter or number"
                        )
                    )
                }

                // Validate device fields
                val validation = DeviceValidator.validateDeviceFields(request.name, request.host, request.port)
                if (validation is ValidationResult.Invalid) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validation.message)
                    )
                }

                // Check for duplicate ID
                if (deviceRepository.exists(request.id)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Device with ID '${request.id}' already exists")
                    )
                }

                // Check for duplicate host:port
                if (deviceRepository.existsByHostAndPort(request.host, request.port)) {
                    return@post call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Device at ${request.host}:${request.port} already registered")
                    )
                }

                val device = UpsDevice(
                    id = request.id,
                    name = request.name,
                    location = request.location,
                    host = request.host,
                    port = request.port,
                    enabled = request.enabled
                )

                try {
                    val created = deviceRepository.insert(device)

                    // Start monitoring if enabled
                    if (created.enabled) {
                        monitorService.addDevice(created)
                    }

                    logger.info("Created device: ${created.id} (${created.name})")
                    call.respond(HttpStatusCode.Created, created)
                } catch (e: Exception) {
                    logger.error("Failed to create device", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to create device: ${e.message}")
                    )
                }
            }

            // PUT /devices/{id} - Update device
            put("/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Device ID is required")
                )

                val existing = deviceRepository.findById(id)
                if (existing == null) {
                    return@put call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Device not found: $id")
                    )
                }

                val request = try {
                    call.receive<UpdateDeviceRequest>()
                } catch (e: Exception) {
                    logger.warn("Invalid update device request: ${e.message}")
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request body: ${e.message}")
                    )
                }

                // Validate device fields
                val validation = DeviceValidator.validateDeviceFields(request.name, request.host, request.port)
                if (validation is ValidationResult.Invalid) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validation.message)
                    )
                }

                // Check for duplicate host:port (excluding this device)
                if (deviceRepository.existsByHostAndPort(request.host, request.port, excludeId = id)) {
                    return@put call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "Another device at ${request.host}:${request.port} already registered")
                    )
                }

                val updated = existing.copy(
                    name = request.name,
                    location = request.location,
                    host = request.host,
                    port = request.port,
                    enabled = request.enabled,
                    updatedAt = Clock.System.now()
                )

                try {
                    val result = deviceRepository.update(updated)
                    if (result != null) {
                        // Update monitoring
                        monitorService.updateDevice(result)

                        logger.info("Updated device: ${result.id}")
                        call.respond(result)
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to update device")
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Failed to update device", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to update device: ${e.message}")
                    )
                }
            }

            // DELETE /devices/{id} - Delete device
            delete("/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Device ID is required")
                )

                // Stop monitoring first
                monitorService.removeDevice(id)

                val deleted = deviceRepository.delete(id)
                if (deleted) {
                    logger.info("Deleted device: $id")
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Device not found: $id")
                    )
                }
            }

            // POST /devices/test - Test device connection
            post("/test") {
                val request = try {
                    call.receive<TestConnectionRequest>()
                } catch (e: Exception) {
                    logger.warn("Invalid test connection request: ${e.message}")
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid request body: ${e.message}")
                    )
                }

                // Validate connection fields
                val validation = DeviceValidator.validateConnectionFields(request.host, request.port)
                if (validation is ValidationResult.Invalid) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to validation.message)
                    )
                }

                try {
                    val output = apcAccessClient.fetchStatusOutput(request.host, request.port)
                    val status = ApcAccessParser.parse(output)

                    if (status.status.isBlank() || status.model.isBlank()) {
                        call.respond(
                            TestConnectionResponse(
                                success = false,
                                message = "Connected but received empty/invalid response",
                                status = null
                            )
                        )
                    } else {
                        call.respond(
                            TestConnectionResponse(
                                success = true,
                                message = "Connection successful - ${status.model} (${status.status})",
                                status = status
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Connection test failed for ${request.host}:${request.port}: ${e.message}")
                    call.respond(
                        TestConnectionResponse(
                            success = false,
                            message = "Connection failed: ${e.message}",
                            status = null
                        )
                    )
                }
            }
        }

        // Status routes with multi-device support
        route("/status") {
            // GET /status/latest - Get latest status for all devices
            get("/latest") {
                val devices = deviceRepository.findAll()
                val latestStatuses = statusRepository.findLatestForAllDevices()

                val results = devices.map { device ->
                    val deviceState = monitorService.getDeviceState(device.id)
                    DeviceWithStatus(
                        device = device,
                        status = latestStatuses[device.id],
                        connectionHealth = when {
                            deviceState?.isConnectionLost?.get() == true -> ConnectionHealth.OFFLINE
                            deviceState?.consecutiveFailures?.get()?.let { it > 0 } == true -> ConnectionHealth.DEGRADED
                            latestStatuses[device.id] == null -> ConnectionHealth.UNKNOWN
                            else -> ConnectionHealth.HEALTHY
                        }
                    )
                }

                call.respond(results)
            }

            // GET /status/latest/{deviceId} - Get latest status for one device
            get("/latest/{deviceId}") {
                val deviceId = call.parameters["deviceId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Device ID is required")
                )

                val device = deviceRepository.findById(deviceId)
                if (device == null) {
                    return@get call.respond(
                        HttpStatusCode.NotFound,
                        mapOf("error" to "Device not found: $deviceId")
                    )
                }

                val status = statusRepository.findLatestByDevice(deviceId)
                val deviceState = monitorService.getDeviceState(deviceId)
                val connectionHealth = when {
                    deviceState?.isConnectionLost?.get() == true -> ConnectionHealth.OFFLINE
                    deviceState?.consecutiveFailures?.get()?.let { it > 0 } == true -> ConnectionHealth.DEGRADED
                    status == null -> ConnectionHealth.UNKNOWN
                    else -> ConnectionHealth.HEALTHY
                }

                call.respond(
                    DeviceWithStatus(
                        device = device,
                        status = status,
                        connectionHealth = connectionHealth
                    )
                )
            }
        }
    }
}
