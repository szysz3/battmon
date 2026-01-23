package com.battmon.data.api

import com.battmon.model.CreateDeviceRequest
import com.battmon.model.DeviceTokenRequest
import com.battmon.model.DeviceWithStatus
import com.battmon.model.TestConnectionRequest
import com.battmon.model.TestConnectionResponse
import com.battmon.model.UpdateDeviceRequest
import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus
import com.battmon.model.UpsStatusHistory
import com.battmon.model.HistoryStatusFilter
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Instant

class BattmonApi {
    private val client = BattmonApiClient.httpClient

    companion object {
        const val DEFAULT_PAGE_SIZE = 500
    }

    // ==================== Device Management ====================

    suspend fun getDevices(): List<UpsDevice> {
        return client.get("/devices").body()
    }

    suspend fun getDevice(id: String): UpsDevice {
        return client.get("/devices/$id").body()
    }

    suspend fun createDevice(request: CreateDeviceRequest): UpsDevice {
        return client.post("/devices") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateDevice(id: String, request: UpdateDeviceRequest): UpsDevice {
        return client.put("/devices/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteDevice(id: String): Boolean {
        return try {
            val response = client.delete("/devices/$id")
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun testConnection(request: TestConnectionRequest): TestConnectionResponse {
        return client.post("/devices/test") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // ==================== Status ====================

    suspend fun getAllLatestStatuses(): List<DeviceWithStatus> {
        return client.get("/status/latest").body()
    }

    suspend fun getLatestStatus(deviceId: String): DeviceWithStatus {
        return client.get("/status/latest/$deviceId").body()
    }

    @Deprecated("Use getAllLatestStatuses() for multi-device support")
    suspend fun getLatestStatus(): UpsStatus {
        // For backward compatibility - get first device's status
        val statuses = getAllLatestStatuses()
        return statuses.firstOrNull()?.status
            ?: throw Exception("No devices configured")
    }

    suspend fun getHistory(
        from: Instant,
        to: Instant,
        deviceId: String? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
        offset: Long = 0,
        statusFilter: HistoryStatusFilter = HistoryStatusFilter.ALL
    ): UpsStatusHistory {
        return client.get("/status/history") {
            parameter("from", from.toString())
            parameter("to", to.toString())
            parameter("limit", limit.toString())
            parameter("offset", offset.toString())
            deviceId?.let { parameter("deviceId", it) }
            if (statusFilter != HistoryStatusFilter.ALL) {
                parameter(
                    "statusFilter",
                    when (statusFilter) {
                        HistoryStatusFilter.ONLINE -> "online"
                        HistoryStatusFilter.OFFLINE_OR_ON_BATTERY -> "offline_or_on_battery"
                        HistoryStatusFilter.ALL -> "all"
                    }
                )
            }
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
