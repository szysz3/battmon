package com.battmon.routes

/**
 * Validation result - either success or an error message.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

/**
 * Validator for device-related input fields.
 *
 * Centralizes validation logic for device creation and updates.
 */
object DeviceValidator {

    /** Maximum allowed length for device name */
    const val MAX_NAME_LENGTH = 100

    /** Minimum port number */
    const val MIN_PORT = 1

    /** Maximum port number */
    const val MAX_PORT = 65535

    /**
     * Pattern for valid hostname:
     * - IPv4: 192.168.1.1
     * - IPv6: [::1] or full format
     * - Hostname: letters, numbers, hyphens, dots (e.g., server.local, my-ups-01)
     */
    private val HOST_PATTERN = Regex(
        """^(?:""" +
            // IPv4
            """(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)""" +
            """|""" +
            // IPv6 (bracketed or simple)
            """\[?(?:[0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}\]?""" +
            """|""" +
            // Hostname (RFC 1123)
            """(?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)*[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?""" +
            """)$"""
    )

    /**
     * Validates device name.
     */
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Name is required")
            name.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("Name must be at most $MAX_NAME_LENGTH characters")
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates host address (IP or hostname).
     */
    fun validateHost(host: String): ValidationResult {
        return when {
            host.isBlank() -> ValidationResult.Invalid("Host is required")
            !HOST_PATTERN.matches(host) -> ValidationResult.Invalid(
                "Invalid host format. Use IP address (e.g., 192.168.1.1) or hostname (e.g., ups.local)"
            )
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates port number.
     */
    fun validatePort(port: Int): ValidationResult {
        return if (port < MIN_PORT || port > MAX_PORT) {
            ValidationResult.Invalid("Port must be between $MIN_PORT and $MAX_PORT")
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates all common device fields (name, host, port).
     * Returns the first validation error or Valid if all pass.
     */
    fun validateDeviceFields(name: String, host: String, port: Int): ValidationResult {
        validateName(name).let { if (it is ValidationResult.Invalid) return it }
        validateHost(host).let { if (it is ValidationResult.Invalid) return it }
        validatePort(port).let { if (it is ValidationResult.Invalid) return it }
        return ValidationResult.Valid
    }

    /**
     * Validates connection test fields (host, port).
     * Returns the first validation error or Valid if all pass.
     */
    fun validateConnectionFields(host: String, port: Int): ValidationResult {
        validateHost(host).let { if (it is ValidationResult.Invalid) return it }
        validatePort(port).let { if (it is ValidationResult.Invalid) return it }
        return ValidationResult.Valid
    }
}
