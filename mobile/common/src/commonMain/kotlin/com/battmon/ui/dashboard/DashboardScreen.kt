package com.battmon.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsStatus
import com.battmon.ui.components.*
import com.battmon.ui.state.UiState
import com.battmon.ui.theme.BatteryGradient
import com.battmon.ui.theme.LoadGradient
import com.battmon.ui.theme.StatusGradient
import com.battmon.ui.theme.TimeGradient
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Initial, is UiState.Loading -> {
                LoadingShimmer(modifier = Modifier.padding(16.dp))
            }

            is UiState.Success -> {
                DashboardContent(status = state.data)
            }

            is UiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
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
    GlassCard(
        gradient = StatusGradient,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        padding = 26.dp,
        elevation = 12.dp
    ) {
        // Small title
        CardLabel(text = "UPS STATUS", color = Color.White.copy(alpha = 0.65f))

        Spacer(modifier = Modifier.height(24.dp))

        // Emphasized Status Badge
        StatusBadge(status = status.status, large = true)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Updated ${formatTime(status.timestamp)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.2f))
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
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.model,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Self Test
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Self Test",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.selftest ?: "N/A",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
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

    LaunchedEffect(loadPercent) {
        animatedProgress.animateTo(
            targetValue = loadPercent / 100f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    GlassCard(
        gradient = LoadGradient,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        padding = 22.dp,
        elevation = 10.dp
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
                CardLabel(text = "CURRENT LOAD", color = Color.White.copy(alpha = 0.75f))

                Text(
                    text = "$loadPercent%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 40.sp,
                    lineHeight = 40.sp
                )

                Text(
                    text = "${status.nompower?.roundToInt() ?: 0} W",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
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
                    color = Color.White.copy(alpha = 0.9f),
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$loadPercent%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
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

    GlassCard(
        gradient = BatteryGradient,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        padding = 18.dp,
        elevation = 8.dp
    ) {
        CardLabel(text = "BATTERY", color = Color.White.copy(alpha = 0.75f))

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$batteryPercent%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 34.sp,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$actualVoltage / ${nominalVoltage}V",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Compact Time Card (Less Important)
@Composable
private fun CompactTimeCard(status: UpsStatus) {
    val timeLeft = status.timeleft?.roundToInt() ?: 0

    GlassCard(
        gradient = TimeGradient,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        padding = 18.dp,
        elevation = 8.dp
    ) {
        CardLabel(text = "TIME LEFT", color = Color.White.copy(alpha = 0.75f))

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "$timeLeft",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 34.sp,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CardLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp
    )
}

private fun formatTime(instant: kotlinx.datetime.Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hours = localDateTime.hour.toString().padStart(2, '0')
    val minutes = localDateTime.minute.toString().padStart(2, '0')
    val seconds = localDateTime.second.toString().padStart(2, '0')
    return "$hours:$minutes:$seconds"
}
