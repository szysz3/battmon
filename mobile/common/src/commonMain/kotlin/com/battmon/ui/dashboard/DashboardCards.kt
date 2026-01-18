package com.battmon.ui.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battmon.model.UpsStatus
import com.battmon.ui.components.GlassCard
import com.battmon.ui.components.Label
import com.battmon.ui.components.StatusBadge
import com.battmon.ui.components.glassAccentShimmer
import com.battmon.ui.components.styledCard
import com.battmon.ui.theme.AccentPink
import com.battmon.ui.theme.PrimaryBlue
import com.battmon.ui.theme.SecondaryTeal
import com.battmon.ui.theme.cardGradient
import com.battmon.util.DateTimeFormatter
import com.battmon.util.StatusMapper
import kotlin.math.roundToInt

@Composable
internal fun HeroStatusCard(status: UpsStatus) {
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

@Composable
internal fun ModernLoadCard(status: UpsStatus) {
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
                    fontSize = 15.sp,
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

@Composable
internal fun CompactBatteryCard(status: UpsStatus) {
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

@Composable
internal fun CompactTimeCard(status: UpsStatus) {
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
