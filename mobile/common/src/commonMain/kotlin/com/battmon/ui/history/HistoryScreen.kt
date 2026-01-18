package com.battmon.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.EmptyState
import com.battmon.ui.components.ErrorView
import com.battmon.ui.state.UiState

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel { HistoryViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()
    val isRefreshing = uiState is UiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.reloadHistory() }
    )
    val density = LocalDensity.current
    val listItemSpacing = 14.dp
    val filterCardVerticalPadding = 12.dp
    var filterCardHeight by remember { mutableStateOf(0.dp) }
    val listTopInset = filterCardHeight + listItemSpacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
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
                            icon = Icons.AutoMirrored.Filled.List
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

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = listTopInset),
            contentColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )

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

private sealed class HistoryDisplayState {
    data object Loading : HistoryDisplayState()
    data object Empty : HistoryDisplayState()
    data class Items(val items: List<UpsStatus>) : HistoryDisplayState()
    data class Error(val message: String) : HistoryDisplayState()
}
