package com.battmon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsStatus
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

class HistoryViewModel : ViewModel() {
    private val repository = UpsRepository()

    private val _uiState = MutableStateFlow<UiState<List<UpsStatus>>>(UiState.Initial)
    val uiState: StateFlow<UiState<List<UpsStatus>>> = _uiState.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val now = Clock.System.now()
        val yesterday = now.minus(1.days)
        loadHistory(yesterday, now)
    }

    fun loadHistory(from: Instant, to: Instant) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getHistory(from, to)
            _uiState.value = when (result) {
                is UiState.Success -> UiState.Success(result.data.data)
                is UiState.Error -> result
                else -> UiState.Error("Unknown error")
            }
        }
    }

    fun toggleExpanded(id: Long) {
        _expandedIds.value = if (id in _expandedIds.value) {
            _expandedIds.value - id
        } else {
            _expandedIds.value + id
        }
    }
}
