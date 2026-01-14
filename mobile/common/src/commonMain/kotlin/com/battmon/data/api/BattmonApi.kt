package com.battmon.data.api

import com.battmon.model.DeviceTokenRequest
import com.battmon.model.UpsStatus
import com.battmon.model.UpsStatusHistory
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Instant

class BattmonApi {
    private val client = BattmonApiClient.httpClient

    suspend fun getLatestStatus(): UpsStatus {
        return client.get("/status/latest").body()
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 500
    }

    /**
     * Fetches UPS status history with pagination support.
     *
     * @param from Start timestamp (inclusive)
     * @param to End timestamp (inclusive)
     * @param limit Maximum number of records to return (default: 500)
     * @param offset Number of records to skip for pagination (default: 0)
     * @return UpsStatusHistory containing the data and pagination info
     */
    suspend fun getHistory(
        from: Instant,
        to: Instant,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Long = 0
    ): UpsStatusHistory {
        return client.get("/status/history") {
            parameter("from", from.toString())
            parameter("to", to.toString())
            parameter("limit", limit.toString())
            parameter("offset", offset.toString())
        }.body()
    }

    suspend fun registerDeviceToken(request: DeviceTokenRequest): Boolean {
        return try {
            val response = client.post("/notifications/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to register device token: ${e.message}")
            false
        }
    }

    suspend fun unregisterDeviceToken(fcmToken: String): Boolean {
        return try {
            val response = client.delete("/notifications/unregister") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("fcmToken" to fcmToken))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to unregister device token: ${e.message}")
            false
        }
    }

    suspend fun sendTestNotification(fcmToken: String): Boolean {
        return try {
            val response = client.post("/notifications/test") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("fcmToken" to fcmToken))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to send test notification: ${e.message}")
            false
        }
    }
}
