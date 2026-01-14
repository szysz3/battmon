package com.battmon.service

import com.battmon.config.EmailConfig
import com.battmon.model.UpsStatus
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.*

class EmailNotificationService(
    private val config: EmailConfig
) {
    private val logger = LoggerFactory.getLogger(EmailNotificationService::class.java)
    private val session: Session? by lazy {
        if (!config.enabled) {
            null
        } else {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", config.smtpStartTls.toString())
                put("mail.smtp.host", config.smtpHost)
                put("mail.smtp.port", config.smtpPort.toString())
            }

            Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.smtpUsername, config.smtpPassword)
                }
            })
        }
    }

    suspend fun sendStatusAlertEmail(status: UpsStatus, apcAccessOutput: String) {
        if (!config.enabled) {
            logger.debug("Email notifications disabled, skipping status alert email")
            return
        }

        if (config.to.isBlank()) {
            logger.warn("Email recipient not configured, skipping email")
            return
        }

        val subject = "⚠️ UPS Status Alert: ${status.status}"
        val body = buildStatusAlertBody(status, apcAccessOutput)

        sendEmail(subject, body)
    }

    suspend fun sendConnectionLostEmail(consecutiveFailures: Int, lastOutput: String?) {
        if (!config.enabled) {
            logger.debug("Email notifications disabled, skipping connection lost email")
            return
        }

        if (config.to.isBlank()) {
            logger.warn("Email recipient not configured, skipping email")
            return
        }

        val subject = "⚠️ UPS Connection Lost (${consecutiveFailures} failures)"
        val body = buildConnectionLostBody(consecutiveFailures, lastOutput)

        sendEmail(subject, body)
    }

    suspend fun sendConnectionRestoredEmail(previousFailures: Int, currentOutput: String) {
        if (!config.enabled) {
            logger.debug("Email notifications disabled, skipping connection restored email")
            return
        }

        if (config.to.isBlank()) {
            logger.warn("Email recipient not configured, skipping email")
            return
        }

        val subject = "✅ UPS Connection Restored"
        val body = buildConnectionRestoredBody(previousFailures, currentOutput)

        sendEmail(subject, body)
    }

    suspend fun sendRecoveryEmail(status: UpsStatus, previousStatus: String?, apcAccessOutput: String) {
        if (!config.enabled) {
            logger.debug("Email notifications disabled, skipping recovery email")
            return
        }

        if (config.to.isBlank()) {
            logger.warn("Email recipient not configured, skipping email")
            return
        }

        val subject = "✅ UPS Power Restored: ${status.status}"
        val body = buildRecoveryBody(status, previousStatus, apcAccessOutput)

        sendEmail(subject, body)
    }

    private suspend fun sendEmail(subject: String, body: String) = withContext(Dispatchers.IO) {
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(config.from))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.to))
                setSubject(subject)
                setText(body, "UTF-8", "plain")
            }

            Transport.send(message)
            logger.info("Successfully sent email to ${config.to}: $subject")
        } catch (e: Exception) {
            logger.error("Failed to send email", e)
        }
    }

    private fun buildStatusAlertBody(status: UpsStatus, apcAccessOutput: String): String {
        return """
            |UPS Status Alert
            |================
            |
            |Timestamp: ${status.timestamp}
            |Status: ${status.status}
            |
            |Key Metrics:
            |  - Battery Charge: ${status.bcharge?.let { "${it.toInt()}%" } ?: "N/A"}
            |  - Time Left: ${status.timeleft?.let { "${it.toInt()} minutes" } ?: "N/A"}
            |  - Line Voltage: ${status.linev?.let { "${it.toInt()}V" } ?: "N/A"}
            |  - Load: ${status.loadpct?.let { "${it.toInt()}%" } ?: "N/A"}
            |
            |UPS Details:
            |  - Model: ${status.model}
            |  - UPS Name: ${status.upsname}
            |  - Serial: ${status.serialno ?: "N/A"}
            |
            |Full apcaccess Output:
            |${"-".repeat(80)}
            |$apcAccessOutput
            |${"-".repeat(80)}
            |
            |This is an automated notification from BattMon.
        """.trimMargin()
    }

    private fun buildConnectionLostBody(consecutiveFailures: Int, lastOutput: String?): String {
        return """
            |UPS Connection Lost
            |===================
            |
            |Failed to retrieve UPS status after $consecutiveFailures consecutive attempts.
            |
            |Possible causes:
            |  - apcupsd service stopped
            |  - UPS disconnected
            |  - Network/communication issue
            |
            |Action Required:
            |  - Check apcupsd service status
            |  - Verify UPS connection
            |  - Check system logs
            |
            |${if (lastOutput != null) {
                """
                |Last apcaccess Output (before failure):
                |${"-".repeat(80)}
                |$lastOutput
                |${"-".repeat(80)}
                """.trimMargin()
            } else {
                "No previous output available."
            }}
            |
            |This is an automated notification from BattMon.
        """.trimMargin()
    }

    private fun buildConnectionRestoredBody(previousFailures: Int, currentOutput: String): String {
        return """
            |UPS Connection Restored
            |=======================
            |
            |Successfully reconnected to apcupsd after $previousFailures failed attempts.
            |
            |Current apcaccess Output:
            |${"-".repeat(80)}
            |$currentOutput
            |${"-".repeat(80)}
            |
            |This is an automated notification from BattMon.
        """.trimMargin()
    }

    private fun buildRecoveryBody(status: UpsStatus, previousStatus: String?, apcAccessOutput: String): String {
        return """
            |UPS Power Restored
            |==================
            |
            |Timestamp: ${status.timestamp}
            |Current Status: ${status.status}
            |${previousStatus?.let { "Previous Status: $it" } ?: ""}
            |
            |Current Metrics:
            |  - Battery Charge: ${status.bcharge?.let { "${it.toInt()}%" } ?: "N/A"}
            |  - Time Left: ${status.timeleft?.let { "${it.toInt()} minutes" } ?: "N/A"}
            |  - Line Voltage: ${status.linev?.let { "${it.toInt()}V" } ?: "N/A"}
            |  - Load: ${status.loadpct?.let { "${it.toInt()}%" } ?: "N/A"}
            |
            |Full apcaccess Output:
            |${"-".repeat(80)}
            |$apcAccessOutput
            |${"-".repeat(80)}
            |
            |This is an automated notification from BattMon.
        """.trimMargin()
    }
}
