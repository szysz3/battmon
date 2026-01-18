package com.battmon.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.ErrorView
import com.battmon.ui.components.LoadingShimmer
import com.battmon.ui.state.UiState

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState is UiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        val displayState = remember(uiState) {
            when (val state = uiState) {
                is UiState.Initial, is UiState.Loading -> DashboardDisplayState.Loading
                is UiState.Success -> DashboardDisplayState.Success(state.data)
                is UiState.Error -> DashboardDisplayState.Error(state.message)
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
                is DashboardDisplayState.Loading -> {
                    LoadingShimmer(modifier = Modifier.padding(16.dp))
                }

                is DashboardDisplayState.Success -> {
                    DashboardContent(status = state.status)
                }

                is DashboardDisplayState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private sealed class DashboardDisplayState {
    data object Loading : DashboardDisplayState()
    data class Success(val status: UpsStatus) : DashboardDisplayState()
    data class Error(val message: String) : DashboardDisplayState()
}

@Composable
private fun DashboardContent(status: UpsStatus) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HeroStatusCard(status)
        }

        item {
            ModernLoadCard(status)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    CompactBatteryCard(status)
                }

                Box(modifier = Modifier.weight(1f)) {
                    CompactTimeCard(status)
                }
            }
        }
    }
}
