package com.battmon.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsStatus
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val repository = UpsRepository()

    private val _uiState = MutableStateFlow<UiState<UpsStatus>>(UiState.Initial)
    val uiState: StateFlow<UiState<UpsStatus>> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = repository.getLatestStatus()
        }
    }
}
