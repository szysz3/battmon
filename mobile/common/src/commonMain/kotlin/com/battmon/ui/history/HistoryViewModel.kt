package com.battmon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.UpsStatus
import com.battmon.ui.state.UiState
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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
    POSITIVE,
    NEGATIVE
}

data class HistoryFilterState(
    val selectedPreset: HistoryRangePreset,
    val statusFilter: HistoryStatusFilter
)

private data class HistoryRange(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val fromTime: String,
    val toTime: String
)

class HistoryViewModel : ViewModel() {
    private val repository = UpsRepository()

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

    private val _historyItems = MutableStateFlow<List<UpsStatus>>(emptyList())
    val filteredItems: StateFlow<List<UpsStatus>> = combine(_historyItems, filterState) { items, filter ->
        items.filter { status ->
            matchesStatusFilter(status, filter.statusFilter)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val range = rangeForPreset(HistoryRangePreset.LAST_24_HOURS)
        loadHistoryForRange(range.fromDate, range.toDate, range.fromTime, range.toTime)
    }

    fun reloadHistory() {
        val range = rangeForPreset(_filterState.value.selectedPreset)
        loadHistoryForRange(range.fromDate, range.toDate, range.fromTime, range.toTime)
    }

    fun loadHistory(from: Instant, to: Instant) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = withContext(Dispatchers.Default) {
                repository.getHistory(from, to)
            }
            _uiState.value = when (result) {
                is UiState.Success -> {
                    _historyItems.value = result.data.data
                    UiState.Success(result.data.data)
                }
                is UiState.Error -> result
                else -> UiState.Error("Unknown error")
            }
            if (result !is UiState.Success) {
                _historyItems.value = emptyList()
            }
        }
    }

    fun applyPreset(preset: HistoryRangePreset) {
        val range = rangeForPreset(preset)
        _filterState.value = _filterState.value.copy(selectedPreset = preset)
        loadHistoryForRange(range.fromDate, range.toDate, range.fromTime, range.toTime)
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

    private fun loadHistoryForRange(
        fromDate: LocalDate,
        toDate: LocalDate,
        fromTime: String,
        toTime: String
    ) {
        val timeZone = TimeZone.currentSystemDefault()
        val fromTimeParsed = LocalTime.parse(fromTime)
        val toTimeParsed = LocalTime.parse(toTime)
        val fromDateTime = LocalDateTime(
            fromDate.year,
            fromDate.monthNumber,
            fromDate.dayOfMonth,
            fromTimeParsed.hour,
            fromTimeParsed.minute
        )
        val toDateTime = LocalDateTime(
            toDate.year,
            toDate.monthNumber,
            toDate.dayOfMonth,
            toTimeParsed.hour,
            toTimeParsed.minute
        )
        val from = fromDateTime.toInstant(timeZone)
        val to = toDateTime
            .toInstant(timeZone)
            .plus(1.minutes)
            .minus(1.milliseconds)
        loadHistory(from, to)
    }

    private fun rangeForPreset(preset: HistoryRangePreset): HistoryRange {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        return when (preset) {
            HistoryRangePreset.LAST_24_HOURS -> {
                val toDateTime = now.toLocalDateTime(timeZone)
                val fromDateTime = now.minus(24.hours).toLocalDateTime(timeZone)
                HistoryRange(
                    fromDate = fromDateTime.date,
                    toDate = toDateTime.date,
                    fromTime = formatTime(fromDateTime.time),
                    toTime = formatTime(toDateTime.time)
                )
            }
            HistoryRangePreset.LAST_7_DAYS -> {
                val today = now.toLocalDateTime(timeZone).date
                val fromDate = today.minus(6, DateTimeUnit.DAY)
                HistoryRange(fromDate, today, defaultFromTime(), defaultToTime())
            }
            HistoryRangePreset.LAST_30_DAYS -> {
                val today = now.toLocalDateTime(timeZone).date
                val fromDate = today.minus(29, DateTimeUnit.DAY)
                HistoryRange(fromDate, today, defaultFromTime(), defaultToTime())
            }
        }
    }

    private fun defaultFromTime() = "00:00"

    private fun defaultToTime() = "23:59"

    private fun formatTime(time: LocalTime): String {
        val hour = time.hour.toString().padStart(2, '0')
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    private fun matchesStatusFilter(status: UpsStatus, filter: HistoryStatusFilter): Boolean {
        val normalized = status.status.trim().lowercase()
        val isOnline = normalized.contains("online")
        return when (filter) {
            HistoryStatusFilter.ALL -> true
            HistoryStatusFilter.POSITIVE -> isOnline
            HistoryStatusFilter.NEGATIVE -> !isOnline
        }
    }
}
