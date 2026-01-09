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

    suspend fun getHistory(from: Instant, to: Instant): UiState<UpsStatusHistory> {
        return try {
            val history = api.getHistory(from, to)
            UiState.Success(history)
        } catch (e: Exception) {
            UiState.Error(ErrorMessages.withDetail(ErrorMessages.FAILED_TO_LOAD_HISTORY, e.message))
        }
    }
}
