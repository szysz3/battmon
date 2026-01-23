package com.battmon.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.DeviceWithStatus
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceListViewModel(
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<DeviceWithStatus>>>(UiState.Initial)
    val uiState: StateFlow<UiState<List<DeviceWithStatus>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = when (val current = _uiState.value) {
                is UiState.Success -> current // Keep showing data while refreshing
                else -> UiState.Loading
            }
            _uiState.value = repository.getAllDevicesWithStatus()
            _isRefreshing.value = false
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteDevice(deviceId)) {
                is UiState.Success -> {
                    // Remove from local list immediately
                    val current = _uiState.value
                    if (current is UiState.Success) {
                        _uiState.value = UiState.Success(
                            current.data.filter { it.device.id != deviceId }
                        )
                    }
                }
                is UiState.Error -> {
                    // Could show error toast/snackbar
                }
                else -> {}
            }
        }
    }

}
