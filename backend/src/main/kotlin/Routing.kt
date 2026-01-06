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
            get("/latest") {
                val latest = repository.findLatest()
                if (latest != null) {
                    call.respond(latest)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "No status data available"))
                }
            }

            get("/history") {
                val fromParam = call.request.queryParameters["from"]
                val toParam = call.request.queryParameters["to"]

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

                    val data = repository.findByTimeRange(from, to)
                    call.respond(
                        UpsStatusHistory(
                            data = data,
                            count = data.size,
                            from = fromParam,
                            to = toParam
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
