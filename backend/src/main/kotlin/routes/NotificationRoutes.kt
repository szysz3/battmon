package com.battmon.routes

import com.battmon.database.DeviceTokenRepository
import com.battmon.model.DeviceTokenRequest
import com.battmon.service.FcmNotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureNotificationRoutes(
    deviceTokenRepository: DeviceTokenRepository,
    fcmService: FcmNotificationService
) {
    routing {
        route("/notifications") {
            post("/register") {
                try {
                    val request = call.receive<DeviceTokenRequest>()

                    if (request.fcmToken.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "FCM token cannot be empty")
                        )
                        return@post
                    }

                    val deviceToken = deviceTokenRepository.upsert(
                        com.battmon.model.DeviceToken(
                            fcmToken = request.fcmToken,
                            deviceName = request.deviceName,
                            platform = request.platform
                        )
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "Device token registered successfully",
                            "deviceToken" to deviceToken
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to register device token: ${e.message}")
                    )
                }
            }

            delete("/unregister") {
                try {
                    val request = call.receive<Map<String, String>>()
                    val fcmToken = request["fcmToken"]

                    if (fcmToken.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "FCM token is required")
                        )
                        return@delete
                    }

                    val deleted = deviceTokenRepository.delete(fcmToken)

                    if (deleted) {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("success" to true, "message" to "Device token unregistered successfully")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Device token not found")
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to unregister device token: ${e.message}")
                    )
                }
            }

            post("/test") {
                try {
                    val request = call.receive<Map<String, String>>()
                    val fcmToken = request["fcmToken"]

                    if (fcmToken.isNullOrBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "FCM token is required")
                        )
                        return@post
                    }

                    val success = fcmService.sendTestNotification(fcmToken)

                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            mapOf("success" to true, "message" to "Test notification sent successfully")
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to "Failed to send test notification")
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to send test notification: ${e.message}")
                    )
                }
            }

            get("/devices") {
                try {
                    val devices = deviceTokenRepository.findAll()
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "count" to devices.size,
                            "devices" to devices
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to fetch devices: ${e.message}")
                    )
                }
            }
        }
    }
}
