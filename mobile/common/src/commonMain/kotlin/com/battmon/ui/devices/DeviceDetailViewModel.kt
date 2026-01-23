package com.battmon.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsDevice
import com.battmon.model.DeviceWithStatus
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceDetailViewModel(
    private val deviceId: String,
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    private val _device = MutableStateFlow<UiState<UpsDevice>>(UiState.Initial)
    val device: StateFlow<UiState<UpsDevice>> = _device.asStateFlow()

    private val _status = MutableStateFlow<UiState<DeviceWithStatus>>(UiState.Initial)
    val status: StateFlow<UiState<DeviceWithStatus>> = _status.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadDevice()
        loadStatus()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadDeviceInternal()
            loadStatusInternal()
            _isRefreshing.value = false
        }
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _device.value = UiState.Loading
            loadDeviceInternal()
        }
    }

    private fun loadStatus() {
        viewModelScope.launch {
            _status.value = UiState.Loading
            loadStatusInternal()
        }
    }

    private suspend fun loadDeviceInternal() {
        _device.value = repository.getDevice(deviceId)
    }

    private suspend fun loadStatusInternal() {
        _status.value = repository.getLatestStatus(deviceId)
    }

}
