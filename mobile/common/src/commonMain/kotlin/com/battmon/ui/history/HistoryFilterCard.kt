package com.battmon.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.battmon.model.HistoryStatusFilter
import com.battmon.ui.components.GlassCard
import com.battmon.ui.components.Label
import com.battmon.ui.components.LabelVariant
import com.battmon.ui.components.glassAccentShimmer
import com.battmon.ui.components.styledCard
import com.battmon.ui.theme.filterCardGradient

@Composable
internal fun HistoryFilterCard(
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
