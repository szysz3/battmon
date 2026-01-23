package com.battmon.service

import com.battmon.model.UpsDevice
import com.battmon.model.UpsStatus

/**
 * Notification dispatcher for multi-device mode.
 * Includes device information in all notifications.
 */
interface MultiDeviceNotificationDispatcher {
    suspend fun sendStatusAlert(device: UpsDevice, status: UpsStatus, apcAccessOutput: String)
    suspend fun sendRecoveryAlert(device: UpsDevice, status: UpsStatus, previousStatus: String?, apcAccessOutput: String)
    suspend fun sendConnectionLostAlert(device: UpsDevice, consecutiveFailures: Int, lastOutput: String?)
    suspend fun sendConnectionRestoredAlert(device: UpsDevice, previousFailures: Int, status: UpsStatus)
}

/**
 * Default implementation for multi-device mode.
 * Includes device name and location in notifications.
 */
class DefaultMultiDeviceNotificationDispatcher(
    private val fcmService: FcmNotificationService?,
    private val emailService: EmailNotificationService?
) : MultiDeviceNotificationDispatcher {

    override suspend fun sendStatusAlert(device: UpsDevice, status: UpsStatus, apcAccessOutput: String) {
        fcmService?.sendStatusAlert(device, status)
        emailService?.sendStatusAlertEmail(device, status, apcAccessOutput)
    }

    override suspend fun sendRecoveryAlert(
        device: UpsDevice,
        status: UpsStatus,
        previousStatus: String?,
        apcAccessOutput: String
    ) {
        fcmService?.sendRecoveryAlert(device, status, previousStatus)
        emailService?.sendRecoveryEmail(device, status, previousStatus, apcAccessOutput)
    }

    override suspend fun sendConnectionLostAlert(device: UpsDevice, consecutiveFailures: Int, lastOutput: String?) {
        fcmService?.sendConnectionLostAlert(device, consecutiveFailures)
        emailService?.sendConnectionLostEmail(device, consecutiveFailures, lastOutput)
    }

    override suspend fun sendConnectionRestoredAlert(device: UpsDevice, previousFailures: Int, status: UpsStatus) {
        fcmService?.sendConnectionRestoredAlert(device, previousFailures)
        emailService?.sendConnectionRestoredEmail(device, previousFailures, status)
    }
}
