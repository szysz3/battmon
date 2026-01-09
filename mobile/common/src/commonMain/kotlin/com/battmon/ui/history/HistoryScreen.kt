package com.battmon.ui.history

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.*
import com.battmon.ui.state.UiState
import com.battmon.ui.theme.filterCardGradient
import com.battmon.ui.theme.historyItemGradient
import com.battmon.util.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel { HistoryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val density = LocalDensity.current
    val listItemSpacing = 14.dp
    val filterCardVerticalPadding = 12.dp
    var filterCardHeight by remember { mutableStateOf(0.dp) }
    val listTopInset = filterCardHeight + listItemSpacing

    Box(modifier = Modifier.fillMaxSize()) {
        val displayState = remember(uiState, filteredItems) {
            when (val state = uiState) {
                is UiState.Initial, is UiState.Loading -> HistoryDisplayState.Loading
                is UiState.Error -> HistoryDisplayState.Error(state.message)
                is UiState.Success -> {
                    if (filteredItems.isEmpty()) {
                        HistoryDisplayState.Empty
                    } else {
                        HistoryDisplayState.Items(filteredItems)
                    }
                }
            }
        }

        AnimatedContent(
            targetState = displayState,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) + slideInVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 8 }
                ) togetherWith fadeOut(animationSpec = tween(140))
            },
            modifier = Modifier.fillMaxSize()
        ) { state ->
                when (state) {
                    is HistoryDisplayState.Loading -> {
                    HistoryLoadingList(topInset = listTopInset)
                    }

                is HistoryDisplayState.Empty -> {
                    Box(modifier = Modifier.padding(top = listTopInset)) {
                        EmptyState(
                            title = "No History",
                            description = "No data available for the selected filters",
                            icon = Icons.Rounded.History
                        )
                    }
                }

                is HistoryDisplayState.Items -> {
                    HistoryList(
                        items = state.items,
                        expandedIds = expandedIds,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        topInset = listTopInset
                    )
                }

                is HistoryDisplayState.Error -> {
                    Box(modifier = Modifier.padding(top = listTopInset)) {
                        ErrorView(
                            message = state.message,
                            onRetry = { viewModel.reloadHistory() }
                        )
                    }
                }
            }
        }

        HistoryFilterCard(
            filterState = filterState,
            onPresetSelected = viewModel::applyPreset,
            onStatusSelected = viewModel::updateStatusFilter,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = filterCardVerticalPadding)
                .onGloballyPositioned { coordinates ->
                    filterCardHeight = with(density) { coordinates.size.height.toDp() }
                }
                .zIndex(1f)
        )
    }
}

@Composable
private fun HistoryFilterCard(
    filterState: HistoryFilterState,
    onPresetSelected: (HistoryRangePreset) -> Unit,
    onStatusSelected: (HistoryStatusFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentTone = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .styledCard(cardShape, shadowElevation = 8.dp)
            .glassAccentShimmer(accentTone),
        gradient = filterCardGradient(accentTone),
        elevation = 2.dp,
        cornerRadius = 22.dp,
        padding = 18.dp
    ) {
        Column {
            Label(text = "FILTER RANGE", variant = LabelVariant.SECTION)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = filterState.selectedPreset == HistoryRangePreset.LAST_24_HOURS,
                    onClick = { onPresetSelected(HistoryRangePreset.LAST_24_HOURS) },
                    label = { Text("Last 24h") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filterState.selectedPreset == HistoryRangePreset.LAST_7_DAYS,
                    onClick = { onPresetSelected(HistoryRangePreset.LAST_7_DAYS) },
                    label = { Text("Last 7d") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filterState.selectedPreset == HistoryRangePreset.LAST_30_DAYS,
                    onClick = { onPresetSelected(HistoryRangePreset.LAST_30_DAYS) },
                    label = { Text("Last 30d") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Label(text = "STATUS", variant = LabelVariant.SECTION)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = filterState.statusFilter == HistoryStatusFilter.ALL,
                    onClick = { onStatusSelected(HistoryStatusFilter.ALL) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filterState.statusFilter == HistoryStatusFilter.ONLINE,
                    onClick = { onStatusSelected(HistoryStatusFilter.ONLINE) },
                    label = { Text("Online") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = filterState.statusFilter == HistoryStatusFilter.OFFLINE_OR_ON_BATTERY,
                    onClick = { onStatusSelected(HistoryStatusFilter.OFFLINE_OR_ON_BATTERY) },
                    label = { Text("Other") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTone,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}


@Composable
private fun HistoryList(
    items: List<UpsStatus>,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    topInset: Dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                onToggle = { status.id?.let { onToggleExpand(it) } }
            )
        }
    }
}

@Composable
private fun HistoryLoadingList(
    topInset: Dp
) {
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
private fun HistoryItem(
    status: UpsStatus,
    isExpanded: Boolean,
    onToggle: () -> Unit
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
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
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
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
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

private sealed class HistoryDisplayState {
    data object Loading : HistoryDisplayState()
    data object Empty : HistoryDisplayState()
    data class Items(val items: List<UpsStatus>) : HistoryDisplayState()
    data class Error(val message: String) : HistoryDisplayState()
}
