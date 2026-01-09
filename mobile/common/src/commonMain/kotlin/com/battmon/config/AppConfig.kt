package com.battmon.config

/**
 * Application configuration
 *
 * To change the API URL:
 * - For development: Set apiBaseUrl to your local IP
 * - For production: Update to production server URL
 */
object AppConfig {
    /**
     * Base URL for the Battmon API
     *
     * Default: Local network (development)
     * Change this to your environment:
     * - Local: "http://192.168.50.99:8080"
     * - Production: "https://api.battmon.com"
     */
    const val API_BASE_URL = "http://192.168.50.99:8080"

    /**
     * Network timeout in milliseconds
     */
    const val NETWORK_TIMEOUT_MS = 30000L

    /**
     * Enable detailed logging (disable in production)
     */
    const val ENABLE_LOGGING = true
}
