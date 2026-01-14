package com.battmon.data.repository

import com.battmon.data.api.BattmonApi
import com.battmon.model.UpsStatus
import com.battmon.model.UpsStatusHistory
import com.battmon.ui.state.UiState
import com.battmon.util.ErrorMessages
import kotlinx.datetime.Instant

class UpsRepository {
    private val api = BattmonApi()

    suspend fun getLatestStatus(): UiState<UpsStatus> {
        return try {
            val status = api.getLatestStatus()
            UiState.Success(status)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_STATUS, e.message))
        }
    }

    /**
     * Fetches UPS status history with pagination support.
     *
     * @param from Start timestamp (inclusive)
     * @param to End timestamp (inclusive)
     * @param limit Maximum number of records to return (default: 500)
     * @param offset Number of records to skip for pagination (default: 0)
     * @return UiState containing UpsStatusHistory or error
     */
    suspend fun getHistory(
        from: Instant,
        to: Instant,
        limit: Int = BattmonApi.DEFAULT_PAGE_SIZE,
        offset: Long = 0
    ): UiState<UpsStatusHistory> {
        return try {
            val history = api.getHistory(from, to, limit, offset)
            UiState.Success(history)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_HISTORY, e.message))
        }
    }
}
