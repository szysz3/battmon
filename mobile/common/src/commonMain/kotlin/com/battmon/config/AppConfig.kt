package com.battmon.config

/**
 * Application configuration.
 *
 * IMPORTANT: Before building for production or publishing:
 * 1. Set API_BASE_URL to your actual server address
 * 2. Set ENABLE_LOGGING to false
 *
 * For local development, update the IP to your server's local network address.
 */
object AppConfig {
    /**
     * Base URL for the BattMon API server.
     * Update this to your server's IP address or domain.
     *
     * Examples:
     * - Local development: "http://192.168.1.100:8080"
     * - Production: "https://your-domain.com"
     */
    const val API_BASE_URL = "http://YOUR_SERVER_IP:8080"

    const val NETWORK_TIMEOUT_MS = 30000L

    /**
     * Enable HTTP request/response logging.
     * Set to false for production builds.
     */
    const val ENABLE_LOGGING = false
}
