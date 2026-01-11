package com.battmon.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DeviceToken(
    val id: Long? = null,
    val fcmToken: String,
    val deviceName: String? = null,
    val platform: String = "ios",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val lastSeen: Instant? = null
)

@Serializable
data class DeviceTokenRequest(
    val fcmToken: String,
    val deviceName: String? = null,
    val platform: String = "ios"
)
