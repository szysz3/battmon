package com.battmon.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battmon.model.UpsStatus
import com.battmon.ui.components.GlassCard
import com.battmon.ui.components.Label
import com.battmon.ui.components.LabelVariant
import com.battmon.ui.components.StatusBadge
import com.battmon.ui.components.glassAccentShimmer
import com.battmon.ui.components.styledCard
import com.battmon.ui.theme.historyItemGradient
import com.battmon.util.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
internal fun HistoryList(
    items: List<UpsStatus>,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    topInset: Dp,
    listState: LazyListState,
    isLoadingMore: Boolean,
    deviceNameById: Map<String, String>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(topInset))
        }
        items(items, key = { it.id ?: it.timestamp.toString() }) { status ->
            HistoryItem(
                status = status,
                isExpanded = status.id in expandedIds,
                onToggle = { status.id?.let { onToggleExpand(it) } },
                deviceName = status.upsDeviceId?.let { deviceNameById[it] ?: it }
            )
        }
        if (isLoadingMore) {
            item(key = "loading_more") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
internal fun HistoryLoadingList(topInset: Dp) {
    val cardShape = RoundedCornerShape(22.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(topInset))
        }
        items(3) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, cardShape, clip = false)
                    .clip(cardShape),
                cornerRadius = 22.dp,
                padding = 0.dp,
                elevation = 2.dp
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(22.dp)
                        )
                )
            }
        }
    }
}

@Composable
internal fun HistoryItem(
    status: UpsStatus,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    deviceName: String? = null
) {
    val accentTone = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .styledCard(cardShape)
            .glassAccentShimmer(accentTone, baseAlpha = 0.28f, highlightAlpha = 0.7f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            ) { onToggle() },
        gradient = historyItemGradient(accentTone),
        elevation = 2.dp,
        cornerRadius = 22.dp,
        padding = 18.dp
    ) {
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
                    if (!deviceName.isNullOrBlank()) {
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = DateTimeFormatter.formatTime(status.timestamp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateTimeFormatter.formatDate(status.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(status = status.status)
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip("BATTERY", "${status.bcharge?.roundToInt() ?: 0}%")
                MetricChip("LOAD", "${status.loadpct?.roundToInt() ?: 0}%")
                MetricChip("LINE V", "${status.linev ?: 0.0}V")
            }

        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 160))
        ) {
            ExpandedStatusDetails(status)
        }
    }
}

@Composable
private fun ExpandedStatusDetails(status: UpsStatus) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )

        ExpandedSection("RECORD") {
            DetailRow("Record ID", status.id?.toString() ?: "N/A")
            DetailRow(
                "Timestamp",
                DateTimeFormatter.formatDateTime(status.timestamp)
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
    }
}

@Composable
internal fun MetricChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExpandedSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Label(text = title, variant = LabelVariant.SECTION)
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.9f)
        )
    }
}
