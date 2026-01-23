package com.battmon

import com.battmon.database.UpsStatusRepository
import com.battmon.model.UpsStatusHistory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant

fun Application.configureRouting(repository: UpsStatusRepository) {
    routing {
        get("/") {
            call.respondText("Battery Monitor API - See /status/latest or /status/history")
        }

        route("/status") {
            get("/history") {
                val fromParam = call.request.queryParameters["from"]
                val toParam = call.request.queryParameters["to"]
                val limitParam = call.request.queryParameters["limit"]
                val offsetParam = call.request.queryParameters["offset"]
                val statusFilterParam = call.request.queryParameters["statusFilter"]

                if (fromParam == null || toParam == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Both 'from' and 'to' query parameters are required (ISO-8601 format)")
                    )
                    return@get
                }

                try {
                    val from = Instant.parse(fromParam)
                    val to = Instant.parse(toParam)

                    if (to < from) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "'to' timestamp must be after 'from' timestamp")
                        )
                        return@get
                    }

                    val limit = if (limitParam != null) {
                        limitParam.toIntOrNull() ?: run {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid 'limit' parameter: must be a valid integer")
                            )
                            return@get
                        }
                    } else {
                        1000
                    }

                    val offset = if (offsetParam != null) {
                        offsetParam.toLongOrNull() ?: run {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("error" to "Invalid 'offset' parameter: must be a valid integer")
                            )
                            return@get
                        }
                    } else {
                        0L
                    }

                    if (limit < 1 || limit > 10000) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "limit must be between 1 and 10000")
                        )
                        return@get
                    }

                    if (offset < 0) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "offset must be non-negative")
                        )
                        return@get
                    }

                    val statusFilter = StatusFilter.fromQuery(statusFilterParam)
                    if (statusFilter == null) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to "Invalid 'statusFilter' parameter: use all, online, or offline_or_on_battery"
                            )
                        )
                        return@get
                    }

                    val deviceId = call.request.queryParameters["deviceId"]
                    val data = repository.findByTimeRange(from, to, deviceId, limit, offset, statusFilter)
                    val totalCount = repository.countByTimeRange(from, to, deviceId, statusFilter)

                    call.respond(
                        UpsStatusHistory(
                            data = data,
                            count = data.size,
                            from = fromParam,
                            to = toParam,
                            totalCount = totalCount,
                            limit = limit,
                            offset = offset
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid timestamp format. Use ISO-8601 format (e.g., 2026-01-06T12:00:00Z)")
                    )
                }
            }
        }
    }
}
