package com.battmon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.api.BattmonApi
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsStatus
import com.battmon.ui.state.UiState
import com.battmon.util.ErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours

enum class HistoryRangePreset {
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS
}

enum class HistoryStatusFilter {
    ALL,
    ONLINE,
    OFFLINE_OR_ON_BATTERY
}

data class HistoryFilterState(
    val selectedPreset: HistoryRangePreset,
    val statusFilter: HistoryStatusFilter
)

/**
 * Tracks pagination state for history loading.
 */
data class PaginationState(
    val currentOffset: Long = 0,
    val totalCount: Long = 0,
    val pageSize: Int = BattmonApi.DEFAULT_PAGE_SIZE,
    val isLoadingMore: Boolean = false
) {
    val hasMore: Boolean get() = currentOffset + pageSize < totalCount
    val loadedCount: Int get() = minOf(currentOffset.toInt() + pageSize, totalCount.toInt())
}

class HistoryViewModel(
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<UpsStatus>>>(UiState.Initial)
    val uiState: StateFlow<UiState<List<UpsStatus>>> = _uiState.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    private val _filterState = MutableStateFlow(
        HistoryFilterState(
            selectedPreset = HistoryRangePreset.LAST_24_HOURS,
            statusFilter = HistoryStatusFilter.ALL
        )
    )
    val filterState: StateFlow<HistoryFilterState> = _filterState.asStateFlow()

    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    private val _historyItems = MutableStateFlow<List<UpsStatus>>(emptyList())
    val filteredItems: StateFlow<List<UpsStatus>> = combine(_historyItems, filterState) { items, filter ->
        items.filter { status ->
            matchesStatusFilter(status, filter.statusFilter)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Store current time range for pagination
    private var currentFrom: Instant = Clock.System.now().minus(24.hours)
    private var currentTo: Instant = Clock.System.now()

    init {
        val (from, to) = rangeForPreset(HistoryRangePreset.LAST_24_HOURS)
        loadHistory(from, to)
    }

    fun reloadHistory() {
        val (from, to) = rangeForPreset(_filterState.value.selectedPreset)
        loadHistory(from, to)
    }

    fun loadHistory(from: Instant, to: Instant) {
        // Reset pagination for new query
        currentFrom = from
        currentTo = to
        _paginationState.value = PaginationState()
        _historyItems.value = emptyList()

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = withContext(Dispatchers.Default) {
                repository.getHistory(from, to, BattmonApi.DEFAULT_PAGE_SIZE, 0)
            }
            _uiState.value = when (result) {
                is UiState.Success -> {
                    _historyItems.value = result.data.data
                    _paginationState.value = PaginationState(
                        currentOffset = 0,
                        totalCount = result.data.totalCount ?: result.data.data.size.toLong(),
                        pageSize = result.data.limit ?: BattmonApi.DEFAULT_PAGE_SIZE
                    )
                    UiState.Success(result.data.data)
                }
                is UiState.Error -> result
                else -> UiState.Error(ErrorMessages.UNKNOWN_ERROR)
            }
            if (result !is UiState.Success) {
                _historyItems.value = emptyList()
            }
        }
    }

    /**
     * Loads the next page of history data.
     * Call this when the user scrolls to the bottom of the list.
     */
    fun loadMore() {
        val pagination = _paginationState.value
        if (!pagination.hasMore || pagination.isLoadingMore) {
            return
        }

        val nextOffset = pagination.currentOffset + pagination.pageSize

        viewModelScope.launch {
            _paginationState.value = pagination.copy(isLoadingMore = true)

            val result = withContext(Dispatchers.Default) {
                repository.getHistory(currentFrom, currentTo, pagination.pageSize, nextOffset)
            }

            when (result) {
                is UiState.Success -> {
                    // Append new items to existing list
                    _historyItems.value = _historyItems.value + result.data.data
                    _paginationState.value = PaginationState(
                        currentOffset = nextOffset,
                        totalCount = result.data.totalCount ?: pagination.totalCount,
                        pageSize = result.data.limit ?: pagination.pageSize,
                        isLoadingMore = false
                    )
                    _uiState.value = UiState.Success(_historyItems.value)
                }
                is UiState.Error -> {
                    // Keep existing data, just stop loading
                    _paginationState.value = pagination.copy(isLoadingMore = false)
                }
                else -> {
                    _paginationState.value = pagination.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun applyPreset(preset: HistoryRangePreset) {
        _filterState.value = _filterState.value.copy(selectedPreset = preset)
        val (from, to) = rangeForPreset(preset)
        loadHistory(from, to)
    }

    fun updateStatusFilter(filter: HistoryStatusFilter) {
        _filterState.value = _filterState.value.copy(statusFilter = filter)
    }

    fun toggleExpanded(id: Long) {
        _expandedIds.value = if (id in _expandedIds.value) {
            _expandedIds.value - id
        } else {
            _expandedIds.value + id
        }
    }

    private fun rangeForPreset(preset: HistoryRangePreset): Pair<Instant, Instant> {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        return when (preset) {
            HistoryRangePreset.LAST_24_HOURS -> {
                val from = now.minus(24.hours)
                val to = now.plus(1.minutes).minus(1.milliseconds)
                from to to
            }
            HistoryRangePreset.LAST_7_DAYS -> {
                val today = now.toLocalDateTime(timeZone).date
                val fromDate = today.minus(6, DateTimeUnit.DAY)
                val from = LocalDateTime(fromDate, LocalTime(0, 0)).toInstant(timeZone)
                val to = LocalDateTime(today, LocalTime(23, 59)).toInstant(timeZone).plus(1.minutes).minus(1.milliseconds)
                from to to
            }
            HistoryRangePreset.LAST_30_DAYS -> {
                val today = now.toLocalDateTime(timeZone).date
                val fromDate = today.minus(29, DateTimeUnit.DAY)
                val from = LocalDateTime(fromDate, LocalTime(0, 0)).toInstant(timeZone)
                val to = LocalDateTime(today, LocalTime(23, 59)).toInstant(timeZone).plus(1.minutes).minus(1.milliseconds)
                from to to
            }
        }
    }

    private fun matchesStatusFilter(status: UpsStatus, filter: HistoryStatusFilter): Boolean {
        val normalized = status.status.trim().lowercase()
        val isOnline = normalized.contains("online")
        return when (filter) {
            HistoryStatusFilter.ALL -> true
            HistoryStatusFilter.ONLINE -> isOnline
            HistoryStatusFilter.OFFLINE_OR_ON_BATTERY -> !isOnline
        }
    }
}
