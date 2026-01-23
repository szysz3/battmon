package com.battmon.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.battmon.model.ConnectionHealth
import com.battmon.model.DeviceWithStatus
import com.battmon.ui.components.GlassCard
import com.battmon.ui.components.glassAccentShimmer
import com.battmon.ui.components.styledCard
import com.battmon.ui.theme.filterCardGradient

@Composable
fun DeviceCard(
    deviceWithStatus: DeviceWithStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val device = deviceWithStatus.device
    val status = deviceWithStatus.status

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            ConnectionHealthIndicator(
                health = deviceWithStatus.connectionHealth,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Device info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val location = device.location
                if (!location.isNullOrBlank()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quick metrics
                if (status != null) {
                    QuickMetrics(
                        batteryPercent = status.bcharge?.toInt(),
                        loadPercent = status.loadpct?.toInt(),
                        lineVoltage = status.linev?.toInt()
                    )
                } else {
                    Text(
                        text = if (device.enabled) "Waiting for data..." else "Monitoring disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Status text
            if (status != null) {
                StatusChip(status = status.status)
            }
        }
    }
}

@Composable
fun ConnectionHealthIndicator(
    health: ConnectionHealth,
    modifier: Modifier = Modifier
) {
    val color = when (health) {
        ConnectionHealth.HEALTHY -> Color(0xFF4CAF50)  // Green
        ConnectionHealth.DEGRADED -> Color(0xFFFFC107) // Yellow/Amber
        ConnectionHealth.OFFLINE -> Color(0xFFF44336)  // Red
        ConnectionHealth.UNKNOWN -> Color(0xFF9E9E9E)  // Gray
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun QuickMetrics(
    batteryPercent: Int?,
    loadPercent: Int?,
    lineVoltage: Int?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        batteryPercent?.let {
            MetricChip(label = "Batt", value = "$it%")
        }
        loadPercent?.let {
            MetricChip(label = "Load", value = "$it%")
        }
        lineVoltage?.let {
            MetricChip(label = "Line", value = "${it}V")
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    val isOnline = status.contains("ONLINE", ignoreCase = true)
    val backgroundColor = if (isOnline) {
        Color(0xFF4CAF50).copy(alpha = 0.15f)
    } else {
        Color(0xFFF44336).copy(alpha = 0.15f)
    }
    val textColor = if (isOnline) {
        Color(0xFF4CAF50)
    } else {
        Color(0xFFF44336)
    }

    val displayText = when {
        status.contains("ONLINE", ignoreCase = true) -> "Online"
        status.contains("ONBATT", ignoreCase = true) -> "On Battery"
        status.contains("LOWBATT", ignoreCase = true) -> "Low Battery"
        else -> status.take(10)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun EmptyDeviceList(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentTone = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .styledCard(cardShape)
                .glassAccentShimmer(accentTone),
            gradient = filterCardGradient(accentTone),
            elevation = 2.dp,
            cornerRadius = 24.dp,
            padding = 22.dp
        ) {
            Text(
                text = "No UPS Devices",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add your first UPS to start monitoring power and battery health.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddDevice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Device")
            }
        }
    }
}
