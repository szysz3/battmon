package com.battmon.data.repository

import com.battmon.data.api.BattmonApi
import com.battmon.model.CreateDeviceRequest
import com.battmon.model.DeviceWithStatus
import com.battmon.model.TestConnectionRequest
import com.battmon.model.TestConnectionResponse
import com.battmon.model.UpdateDeviceRequest
import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus
import com.battmon.model.UpsStatusHistory
import com.battmon.ui.state.UiState
import com.battmon.util.ErrorMessages
import com.battmon.model.HistoryStatusFilter
import kotlinx.datetime.Instant

class UpsRepository {
    private val api = BattmonApi()

    // ==================== Device Management ====================

    suspend fun getDevices(): UiState<List<UpsDevice>> {
        return try {
            val devices = api.getDevices()
            UiState.Success(devices)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Failed to load devices", e.message))
        }
    }

    suspend fun getDevice(id: String): UiState<UpsDevice> {
        return try {
            val device = api.getDevice(id)
            UiState.Success(device)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Failed to load device", e.message))
        }
    }

    suspend fun createDevice(request: CreateDeviceRequest): UiState<UpsDevice> {
        return try {
            val device = api.createDevice(request)
            UiState.Success(device)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Failed to create device", e.message))
        }
    }

    suspend fun updateDevice(id: String, request: UpdateDeviceRequest): UiState<UpsDevice> {
        return try {
            val device = api.updateDevice(id, request)
            UiState.Success(device)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Failed to update device", e.message))
        }
    }

    suspend fun deleteDevice(id: String): UiState<Boolean> {
        return try {
            val success = api.deleteDevice(id)
            if (success) {
                UiState.Success(true)
            } else {
                UiState.Error("Failed to delete device")
            }
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Failed to delete device", e.message))
        }
    }

    suspend fun testConnection(host: String, port: Int): UiState<TestConnectionResponse> {
        return try {
            val result = api.testConnection(TestConnectionRequest(host, port))
            UiState.Success(result)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail("Connection test failed", e.message))
        }
    }

    // ==================== Status ====================

    suspend fun getAllDevicesWithStatus(): UiState<List<DeviceWithStatus>> {
        return try {
            val statuses = api.getAllLatestStatuses()
            UiState.Success(statuses)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_STATUS, e.message))
        }
    }

    suspend fun getLatestStatus(deviceId: String): UiState<DeviceWithStatus> {
        return try {
            val status = api.getLatestStatus(deviceId)
            UiState.Success(status)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_STATUS, e.message))
        }
    }

    @Deprecated("Use getAllDevicesWithStatus() for multi-device support")
    suspend fun getLatestStatus(): UiState<UpsStatus> {
        return try {
            @Suppress("DEPRECATION")
            val status = api.getLatestStatus()
            UiState.Success(status)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_STATUS, e.message))
        }
    }

    suspend fun getHistory(
        from: Instant,
        to: Instant,
        deviceId: String? = null,
        limit: Int = BattmonApi.DEFAULT_PAGE_SIZE,
        offset: Long = 0,
        statusFilter: HistoryStatusFilter = HistoryStatusFilter.ALL
    ): UiState<UpsStatusHistory> {
        return try {
            val history = api.getHistory(from, to, deviceId, limit, offset, statusFilter)
            UiState.Success(history)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_HISTORY, e.message))
        }
    }
}
