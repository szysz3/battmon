package com.battmon.ui.history

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.*
import com.battmon.ui.state.UiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel { HistoryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Initial, is UiState.Loading -> {
                LoadingShimmer(modifier = Modifier.padding(16.dp))
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        title = "No History",
                        description = "No data available for the selected time range"
                    )
                } else {
                    HistoryList(
                        items = state.data,
                        expandedIds = expandedIds,
                        onToggleExpand = { viewModel.toggleExpanded(it) }
                    )
                }
            }

            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadHistory() }
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    items: List<UpsStatus>,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id ?: it.timestamp.toString() }) { status ->
            HistoryItem(
                status = status,
                isExpanded = status.id in expandedIds,
                onToggle = { status.id?.let { onToggleExpand(it) } }
            )
        }
    }
}

@Composable
private fun HistoryItem(
    status: UpsStatus,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val surfaceTone = MaterialTheme.colorScheme.surface
    val variantTone = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceTone = MaterialTheme.colorScheme.onSurface
    val accentTone = MaterialTheme.colorScheme.primary
    val isLightSurface = surfaceTone.luminance() > 0.5f
    val cardGradient = if (isLightSurface) {
        Brush.linearGradient(
            colors = listOf(
                lerp(variantTone, onSurfaceTone, 0.04f),
                lerp(variantTone, accentTone, 0.08f),
                lerp(variantTone, onSurfaceTone, 0.2f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                surfaceTone.copy(alpha = 0.95f),
                variantTone.copy(alpha = 0.9f),
                surfaceTone.copy(alpha = 0.7f)
            )
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onToggle() },
        gradient = cardGradient,
        elevation = 6.dp,
        cornerRadius = 20.dp,
        padding = 18.dp
    ) {
        // Collapsed view
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTime(status.timestamp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDate(status.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(status = status.status)
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key metrics in collapsed view
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip("BATTERY", "${status.bcharge?.roundToInt() ?: 0}%")
                MetricChip("LOAD", "${status.loadpct?.roundToInt() ?: 0}%")
                MetricChip("LINE V", "${status.linev ?: 0.0}V")
            }

        }

        // Expanded view
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 160))
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                ExpandedSection("RECORD") {
                    DetailRow("Record ID", status.id?.toString() ?: "N/A")
                    DetailRow(
                        "Timestamp",
                        "${formatDate(status.timestamp)} ${formatTime(status.timestamp)}"
                    )
                }

                ExpandedSection("STATUS") {
                    DetailRow("Status", status.status)
                    DetailRow("Status Flag", status.statflag ?: "N/A")
                }

                ExpandedSection("IDENTIFICATION") {
                    DetailRow("UPS Name", status.upsname)
                    DetailRow("Model", status.model)
                    DetailRow("Serial Number", status.serialno ?: "N/A")
                    DetailRow("Cable", status.cable)
                    DetailRow("Driver", status.driver)
                    DetailRow("UPS Mode", status.upsmode)
                }

                ExpandedSection("POWER METRICS") {
                    DetailRow("Line Voltage", "${status.linev ?: 0.0} V")
                    DetailRow("Load", "${status.loadpct ?: 0.0}%")
                    DetailRow("Battery Charge", "${status.bcharge ?: 0.0}%")
                    DetailRow("Time Left", "${status.timeleft ?: 0.0} min")
                    DetailRow("Battery Voltage", "${status.battv ?: 0.0} V")
                    DetailRow("Nominal Input", "${status.nominv ?: 0.0} V")
                    DetailRow("Nominal Battery", "${status.nombattv ?: 0.0} V")
                    DetailRow("Nominal Power", "${status.nompower ?: 0.0} W")
                }

                ExpandedSection("TRANSFER INFO") {
                    DetailRow("Low Transfer Voltage", "${status.lotrans ?: 0.0} V")
                    DetailRow("High Transfer Voltage", "${status.hitrans ?: 0.0} V")
                    DetailRow("Sensitivity", status.sense ?: "N/A")
                    DetailRow("Last Transfer", status.lastxfer ?: "None")
                    DetailRow("Transfer Count", "${status.numxfers ?: 0}")
                    DetailRow("Time on Battery", "${status.tonbatt ?: 0}s")
                    DetailRow("Cumulative on Battery", "${status.cumonbatt ?: 0}s")
                    DetailRow("Transfer Off Battery", status.xoffbatt ?: "N/A")
                }

                ExpandedSection("BATTERY SETTINGS") {
                    DetailRow("Min Battery Charge", "${status.mbattchg ?: 0}%")
                    DetailRow("Min Time Left", "${status.mintimel ?: 0} min")
                    DetailRow("Max Time", "${status.maxtime ?: 0}s")
                    DetailRow("Battery Date", status.battdate ?: "N/A")
                    DetailRow("Self Test", status.selftest ?: "N/A")
                }

                ExpandedSection("SYSTEM INFO") {
                    DetailRow("Hostname", status.hostname)
                    DetailRow("APC Version", status.apc)
                    DetailRow("Daemon Version", status.version)
                    DetailRow("Start Time", status.starttime)
                    DetailRow("Master", status.master ?: "N/A")
                    DetailRow("Master Update", status.masterupd ?: "N/A")
                    DetailRow("Date", status.date)
                    DetailRow("End APC", status.endApc)
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExpandedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionLabel(text = title)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatTime(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    val seconds = localDateTime.second.toString().padStart(2, '0')
    return "$hours:$minutes:$seconds"
}

private fun formatDate(instant: Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return localDateTime.date.toString()
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
