package com.battmon.data.api

import com.battmon.model.UpsStatus
import com.battmon.model.UpsStatusHistory
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.datetime.Instant

class BattmonApi {
    private val client = BattmonApiClient.httpClient

    suspend fun getLatestStatus(): UpsStatus {
        return client.get("/status/latest").body()
    }

    suspend fun getHistory(from: Instant, to: Instant): UpsStatusHistory {
        return client.get("/status/history") {
            parameter("from", from.toString())
            parameter("to", to.toString())
        }.body()
    }
}
