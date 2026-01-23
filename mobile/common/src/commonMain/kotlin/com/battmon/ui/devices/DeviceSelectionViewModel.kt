package com.battmon.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsDevice
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceSelectionViewModel(
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    private val _devices = MutableStateFlow<UiState<List<UpsDevice>>>(UiState.Initial)
    val devices: StateFlow<UiState<List<UpsDevice>>> = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Boolean>>(UiState.Initial)
    val deleteState: StateFlow<UiState<Boolean>> = _deleteState.asStateFlow()

    init {
        refreshDevices()
    }

    fun refreshDevices() {
        viewModelScope.launch {
            _devices.value = UiState.Loading
            when (val result = repository.getDevices()) {
                is UiState.Success -> {
                    _devices.value = result
                    val currentSelection = _selectedDeviceId.value
                    if (currentSelection == null || result.data.none { it.id == currentSelection }) {
                        _selectedDeviceId.value = result.data.firstOrNull()?.id
                    }
                }
                is UiState.Error -> {
                    _devices.value = result
                    _selectedDeviceId.value = null
                }
                else -> {
                    _devices.value = UiState.Error("Failed to load devices")
                    _selectedDeviceId.value = null
                }
            }
        }
    }

    fun selectDevice(deviceId: String?) {
        if (_selectedDeviceId.value != deviceId) {
            _selectedDeviceId.value = deviceId
        }
    }

    fun deleteSelectedDevice() {
        val deviceId = _selectedDeviceId.value ?: return
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            _deleteState.value = repository.deleteDevice(deviceId)
            if (_deleteState.value is UiState.Success) {
                refreshDevices()
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Initial
    }
}
