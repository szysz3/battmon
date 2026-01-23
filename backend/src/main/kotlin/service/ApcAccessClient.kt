package com.battmon.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Client for fetching UPS status via apcaccess command.
 */
interface ApcAccessClient {
    /**
     * Fetch status output from a specific host:port.
     * Used for multi-device monitoring.
     *
     * @param host IP address or hostname of the apcupsd NIS server
     * @param port Port number (typically 3551)
     * @return Raw apcaccess output string
     */
    suspend fun fetchStatusOutput(host: String, port: Int): String
}

class HttpApcAccessClient(
    private val proxyBaseUrl: String,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) : ApcAccessClient {
    private val logger = LoggerFactory.getLogger(HttpApcAccessClient::class.java)
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

    override suspend fun fetchStatusOutput(host: String, port: Int): String = withContext(Dispatchers.IO) {
        val requestUri = buildProxyUri(host, port)
        logger.debug("Requesting apcaccess via proxy: $requestUri")

        val request = HttpRequest.newBuilder()
            .uri(requestUri)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .GET()
            .build()

        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                throw ApcAccessException(
                    "apcaccess proxy failed (status=${response.statusCode()}): ${response.body().trim()}"
                )
            }
            response.body()
        } catch (e: Exception) {
            if (e is ApcAccessException) {
                throw e
            }
            throw ApcAccessException("apcaccess proxy request failed: ${e.message}", e)
        }
    }

    private fun buildProxyUri(host: String, port: Int): URI {
        val base = proxyBaseUrl.trimEnd('/')
        val encodedHost = URLEncoder.encode(host, StandardCharsets.UTF_8)
        val encodedPort = URLEncoder.encode(port.toString(), StandardCharsets.UTF_8)
        return URI.create("$base/apcaccess?host=$encodedHost&port=$encodedPort")
    }

    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 7L
    }
}

/**
 * Exception thrown when apcaccess command fails.
 */
class ApcAccessException(message: String, cause: Throwable? = null) : Exception(message, cause)
