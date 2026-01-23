package com.battmon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.api.BattmonApi
import com.battmon.data.repository.UpsRepository
import com.battmon.model.HistoryStatusFilter
import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus
import com.battmon.ui.state.UiState
import com.battmon.util.ErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
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

data class HistoryFilterState(
    val selectedPreset: HistoryRangePreset,
    val statusFilter: HistoryStatusFilter,
    val selectedDeviceId: String? = null
)

data class PaginationState(
    val currentOffset: Long = 0,
    val totalCount: Long? = null,
    val pageSize: Int = BattmonApi.DEFAULT_PAGE_SIZE,
    val lastPageSize: Int = 0,
    val isLoadingMore: Boolean = false
) {
    val hasMore: Boolean
        get() = when {
            totalCount != null -> currentOffset + lastPageSize < totalCount
            else -> lastPageSize == pageSize
        }
    val loadedCount: Int
        get() = if (totalCount != null) {
            minOf(currentOffset.toInt() + lastPageSize, totalCount.toInt())
        } else {
            currentOffset.toInt() + lastPageSize
        }
}

class HistoryViewModel(
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    private val _devices = MutableStateFlow<List<UpsDevice>>(emptyList())
    val devices: StateFlow<List<UpsDevice>> = _devices.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<UpsStatus>>>(UiState.Initial)
    val uiState: StateFlow<UiState<List<UpsStatus>>> = _uiState.asStateFlow()

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds.asStateFlow()

    private val _filterState = MutableStateFlow(
        HistoryFilterState(
            selectedPreset = HistoryRangePreset.LAST_24_HOURS,
            statusFilter = HistoryStatusFilter.ALL,
            selectedDeviceId = null
        )
    )
    val filterState: StateFlow<HistoryFilterState> = _filterState.asStateFlow()

    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()

    private val _historyItems = MutableStateFlow<List<UpsStatus>>(emptyList())
    val filteredItems: StateFlow<List<UpsStatus>> = _historyItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var currentFrom: Instant = Clock.System.now().minus(24.hours)
    private var currentTo: Instant = Clock.System.now()

    init {
        loadDevices()
        val (from, to) = rangeForPreset(HistoryRangePreset.LAST_24_HOURS)
        loadHistory(from, to)
    }

    fun reloadHistory() {
        val (from, to) = rangeForPreset(_filterState.value.selectedPreset)
        loadHistory(from, to)
    }

    fun loadHistory(from: Instant, to: Instant) {
        currentFrom = from
        currentTo = to
        _paginationState.value = PaginationState()
        _historyItems.value = emptyList()

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = withContext(Dispatchers.Default) {
                repository.getHistory(
                    from = from,
                    to = to,
                    deviceId = _filterState.value.selectedDeviceId,
                    limit = BattmonApi.DEFAULT_PAGE_SIZE,
                    offset = 0L,
                    statusFilter = _filterState.value.statusFilter
                )
            }
            _uiState.value = when (result) {
                is UiState.Success -> {
                    _historyItems.value = result.data.data
                    _paginationState.value = PaginationState(
                        currentOffset = 0,
                        totalCount = result.data.totalCount,
                        pageSize = result.data.limit ?: BattmonApi.DEFAULT_PAGE_SIZE,
                        lastPageSize = result.data.data.size
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

    fun loadMore() {
        val pagination = _paginationState.value
        if (!pagination.hasMore || pagination.isLoadingMore) {
            return
        }

        val nextOffset = pagination.currentOffset + pagination.pageSize

        viewModelScope.launch {
            _paginationState.value = pagination.copy(isLoadingMore = true)

            val result = withContext(Dispatchers.Default) {
                repository.getHistory(
                    from = currentFrom,
                    to = currentTo,
                    deviceId = _filterState.value.selectedDeviceId,
                    limit = pagination.pageSize,
                    offset = nextOffset,
                    statusFilter = _filterState.value.statusFilter
                )
            }

            when (result) {
                is UiState.Success -> {
                    _historyItems.value = _historyItems.value + result.data.data
                    _paginationState.value = PaginationState(
                        currentOffset = nextOffset,
                        totalCount = result.data.totalCount ?: pagination.totalCount,
                        pageSize = result.data.limit ?: pagination.pageSize,
                        lastPageSize = result.data.data.size,
                        isLoadingMore = false
                    )
                    _uiState.value = UiState.Success(_historyItems.value)
                }
                is UiState.Error -> {
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
        loadHistory(currentFrom, currentTo)
    }

    fun selectDevice(deviceId: String?) {
        if (_filterState.value.selectedDeviceId != deviceId) {
            _filterState.value = _filterState.value.copy(selectedDeviceId = deviceId)
            loadHistory(currentFrom, currentTo)
        }
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

    private fun loadDevices() {
        viewModelScope.launch {
            when (val result = repository.getDevices()) {
                is UiState.Success -> _devices.value = result.data
                else -> _devices.value = emptyList()
            }
        }
    }

}
