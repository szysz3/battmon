package com.battmon.ui.devices

import androidx.compose.animation.AnimatedContent
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
import com.battmon.model.ConnectionHealth
import com.battmon.model.DeviceWithStatus
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    showTopBar: Boolean = true,
    viewModel: DeviceDetailViewModel = viewModel(key = deviceId) { DeviceDetailViewModel(deviceId) }
) {
    val device by viewModel.device.collectAsState()
    val status by viewModel.status.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    val content: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            AnimatedContent(
                targetState = status,
                label = "device_detail_content"
            ) { statusState ->
                when (statusState) {
                    is UiState.Initial,
                    is UiState.Loading -> {
                        DeviceDetailLoading()
                    }
                    is UiState.Success -> {
                        DeviceDetailContent(deviceWithStatus = statusState.data)
                    }
                    is UiState.Error -> {
                        ErrorView(
                            message = statusState.message,
                            onRetry = { viewModel.refresh() }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        when (val d = device) {
                            is UiState.Success -> {
                                Column {
                                    Text(
                                        text = d.data.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    d.data.location?.let { location ->
                                        Text(
                                            text = location,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                            else -> Text("Device Details")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            content(Modifier.padding(paddingValues))
        }
    } else {
        content(Modifier)
    }
}

@Composable
private fun DeviceDetailContent(
    deviceWithStatus: DeviceWithStatus,
    modifier: Modifier = Modifier
) {
    val status = deviceWithStatus.status
    if (status == null) {
        val healthLabel = when (deviceWithStatus.connectionHealth) {
            ConnectionHealth.OFFLINE -> "Offline"
            ConnectionHealth.DEGRADED -> "Degraded"
            ConnectionHealth.HEALTHY -> "Healthy"
            ConnectionHealth.UNKNOWN -> "Unknown"
        }
        EmptyState(
            title = "No Status Yet",
            description = "We haven't received a successful poll from this device. Connection health: $healthLabel.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Status Card
        item {
            DetailHeroStatusCard(status = status)
        }

        // Load Card
        item {
            DetailLoadCard(status = status)
        }

        // Battery and Time cards in a row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailBatteryCard(
                    status = status,
                    modifier = Modifier.weight(1f)
                )
                DetailTimeCard(
                    status = status,
                    modifier = Modifier.weight(1f)
                )
            }
        }

    }
}

@Composable
private fun DetailHeroStatusCard(status: UpsStatus) {
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
        Label(text = "UPS STATUS", color = mutedText.copy(alpha = 0.9f))
        Spacer(modifier = Modifier.height(24.dp))
        StatusBadge(status = status.status, large = true)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Updated ${DateTimeFormatter.formatTime(status.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = mutedText.copy(alpha = 0.85f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = mutedText.copy(alpha = 0.18f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Model",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardText,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Self Test",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedText.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.selftest ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailLoadCard(status: UpsStatus) {
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
                    fontWeight = FontWeight.Medium
                )
            }
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
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailBatteryCard(status: UpsStatus, modifier: Modifier = Modifier) {
    val batteryPercent = status.bcharge?.roundToInt() ?: 0
    val actualVoltage = status.battv ?: 0.0
    val nominalVoltage = status.nombattv ?: 0.0
    val accent = SecondaryTeal
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        gradient = cardGradient(accent),
        modifier = modifier
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailTimeCard(status: UpsStatus, modifier: Modifier = Modifier) {
    val timeLeft = status.timeleft?.roundToInt() ?: 0
    val accent = AccentPink
    val cardText = MaterialTheme.colorScheme.onSurface
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val cardShape = RoundedCornerShape(22.dp)

    GlassCard(
        gradient = cardGradient(accent),
        modifier = modifier
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeviceDetailLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LoadingShimmer(modifier = Modifier.fillMaxWidth().height(200.dp))
        }
        item {
            LoadingShimmer(modifier = Modifier.fillMaxWidth().height(140.dp))
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LoadingShimmer(modifier = Modifier.weight(1f).height(130.dp))
                LoadingShimmer(modifier = Modifier.weight(1f).height(130.dp))
            }
        }
    }
}
