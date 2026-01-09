package com.battmon.data.api

import com.battmon.config.AppConfig
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object BattmonApiClient {
    val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        if (AppConfig.ENABLE_LOGGING) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = AppConfig.NETWORK_TIMEOUT_MS
            connectTimeoutMillis = AppConfig.NETWORK_TIMEOUT_MS
        }

        defaultRequest {
            url(AppConfig.API_BASE_URL)
        }
    }
}
