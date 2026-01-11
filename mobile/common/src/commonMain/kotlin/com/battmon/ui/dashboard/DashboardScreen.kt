package com.battmon.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.*
import com.battmon.ui.state.UiState
import com.battmon.ui.theme.AccentPink
import com.battmon.ui.theme.PrimaryBlue
import com.battmon.ui.theme.SecondaryTeal
import com.battmon.ui.theme.cardGradient
import com.battmon.util.DateTimeFormatter
import com.battmon.util.StatusMapper
import kotlin.math.roundToInt

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
        // Hero Status Card (Very Important - Large)
        item {
            HeroStatusCard(status)
        }

        // Load Card (Important - Medium)
        item {
            ModernLoadCard(status)
        }

        // Battery & Time Row (Less Important - Small cards side by side)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery Charge Card
                Box(modifier = Modifier.weight(1f)) {
                    CompactBatteryCard(status)
                }

                // Time Left Card
                Box(modifier = Modifier.weight(1f)) {
                    CompactTimeCard(status)
                }
            }
        }
    }
}

// Hero Status Card - Large, prominent display (Very Important)
@Composable
private fun HeroStatusCard(status: UpsStatus) {
    val accent = StatusMapper.getAccentColor(status.status)
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(28.dp)
    GlassCard(
        gradient = cardGradient(accent),
        modifier = Modifier
            .fillMaxWidth()
            .styledCard(cardShape)
            .glassAccentShimmer(accent, drawOnTop = true),
        cornerRadius = 28.dp,
        padding = 26.dp,
        elevation = 2.dp
    ) {
        // Small title
        Label(text = "UPS STATUS", color = mutedText.copy(alpha = 0.9f))

        Spacer(modifier = Modifier.height(24.dp))

        // Emphasized Status Badge
        StatusBadge(status = status.status, large = true)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Updated ${DateTimeFormatter.formatTime(status.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = mutedText.copy(alpha = 0.85f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = mutedText.copy(alpha = 0.18f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Model info - small and subtle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model
            Column {
                Text(
                    text = "Model",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Self Test
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Self Test",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.selftest ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Modern Load Card with Circular Progress (Important)
@Composable
private fun ModernLoadCard(status: UpsStatus) {
    val loadPercent = (status.loadpct?.roundToInt() ?: 0)
    val animatedProgress = remember { Animatable(0f) }
    val accent = PrimaryBlue
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(24.dp)

    LaunchedEffect(loadPercent) {
        animatedProgress.animateTo(
            targetValue = loadPercent / 100f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    GlassCard(
        gradient = cardGradient(accent),
        modifier = Modifier
            .fillMaxWidth()
            .styledCard(cardShape)
            .glassAccentShimmer(accent, drawOnTop = true),
        cornerRadius = 24.dp,
        padding = 22.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Label and percentage
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Label(text = "CURRENT LOAD", color = mutedText.copy(alpha = 0.9f))

                Text(
                    text = "$loadPercent%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = cardText,
                    fontSize = 40.sp,
                    lineHeight = 40.sp
                )

                Text(
                    text = "${status.nompower?.roundToInt() ?: 0} W",
                    style = MaterialTheme.typography.bodyLarge,
                    color = mutedText.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Right side - Circular progress indicator
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier.size(100.dp),
                    color = accent.copy(alpha = 0.9f),
                    strokeWidth = 8.dp,
                    trackColor = accent.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$loadPercent%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = cardText,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .offset(y = (-1).dp)
                )
            }
        }
    }
}

// Compact Battery Card (Less Important)
@Composable
private fun CompactBatteryCard(status: UpsStatus) {
    val batteryPercent = status.bcharge?.roundToInt() ?: 0
    val actualVoltage = status.battv ?: 0.0
    val nominalVoltage = status.nombattv ?: 0.0
    val accent = SecondaryTeal
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        gradient = cardGradient(accent),
        modifier = Modifier
            .fillMaxWidth()
            .styledCard(cardShape)
            .glassAccentShimmer(accent, drawOnTop = true),
        cornerRadius = 22.dp,
        padding = 18.dp,
        elevation = 2.dp
    ) {
        Label(text = "BATTERY", color = mutedText.copy(alpha = 0.9f))

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$batteryPercent%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = cardText,
            fontSize = 34.sp,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$actualVoltage / ${nominalVoltage}V",
            style = MaterialTheme.typography.bodyMedium,
            color = mutedText.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Compact Time Card (Less Important)
@Composable
private fun CompactTimeCard(status: UpsStatus) {
    val timeLeft = status.timeleft?.roundToInt() ?: 0
    val accent = AccentPink
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        gradient = cardGradient(accent),
        modifier = Modifier
            .fillMaxWidth()
            .styledCard(cardShape)
            .glassAccentShimmer(accent, drawOnTop = true),
        cornerRadius = 22.dp,
        padding = 18.dp,
        elevation = 2.dp
    ) {
        Label(text = "TIME LEFT", color = mutedText.copy(alpha = 0.9f))

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$timeLeft",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = cardText,
            fontSize = 34.sp,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = mutedText.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
