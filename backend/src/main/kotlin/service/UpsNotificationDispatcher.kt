package com.battmon.service

import com.battmon.model.UpsStatus

interface UpsNotificationDispatcher {
    suspend fun sendStatusAlert(status: UpsStatus, apcAccessOutput: String)
    suspend fun sendRecoveryAlert(status: UpsStatus, previousStatus: String?, apcAccessOutput: String)
    suspend fun sendConnectionLostAlert(consecutiveFailures: Int, lastOutput: String?)
    suspend fun sendConnectionRestoredAlert(previousFailures: Int, currentOutput: String)
}

class DefaultUpsNotificationDispatcher(
    private val fcmService: FcmNotificationService?,
    private val emailService: EmailNotificationService?
) : UpsNotificationDispatcher {
    override suspend fun sendStatusAlert(status: UpsStatus, apcAccessOutput: String) {
        fcmService?.sendStatusAlert(status)
        emailService?.sendStatusAlertEmail(status, apcAccessOutput)
    }

    override suspend fun sendRecoveryAlert(status: UpsStatus, previousStatus: String?, apcAccessOutput: String) {
        fcmService?.sendRecoveryAlert(status, previousStatus)
        emailService?.sendRecoveryEmail(status, previousStatus, apcAccessOutput)
    }

    override suspend fun sendConnectionLostAlert(consecutiveFailures: Int, lastOutput: String?) {
        fcmService?.sendConnectionLostAlert(consecutiveFailures)
        emailService?.sendConnectionLostEmail(consecutiveFailures, lastOutput)
    }

    override suspend fun sendConnectionRestoredAlert(previousFailures: Int, currentOutput: String) {
        fcmService?.sendConnectionRestoredAlert(previousFailures)
        emailService?.sendConnectionRestoredEmail(previousFailures, currentOutput)
    }
}
