package com.battmon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    gradient: Brush? = null,
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 24.dp,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val baseColor = if (isLightSurface) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    }
    val borderColor = if (isLightSurface) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    Card(
        modifier = modifier,
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLightSurface) Modifier.border(1.dp, borderColor, shape) else Modifier)
                .then(
                    if (gradient != null) {
                        Modifier.background(gradient, shape)
                    } else {
                        Modifier.background(baseColor, shape)
                    }
                )
                .padding(padding),
            content = content
        )
    }
}

@Composable
fun Modifier.glassAccentShimmer(
    accent: Color,
    thickness: Dp = 2.dp,
    cornerInset: Dp = 16.dp,
    shimmerWidth: Dp = 52.dp,
    baseAlpha: Float = 0.22f,
    highlightAlpha: Float = 0.65f,
    durationMillis: Int = 1700,
    drawOnTop: Boolean = false
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "glassAccentShimmer")
    val shimmerPhase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassAccentShimmerPhase"
    )

    val drawShimmer: DrawScope.() -> Unit = {
        val strokeWidth = thickness.toPx()
        val y = strokeWidth / 2f
        val startX = cornerInset.toPx()
        val endX = size.width - cornerInset.toPx()
        val length = endX - startX
        if (length > 0f) {
            drawLine(
                color = accent.copy(alpha = baseAlpha),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            val shimmerPx = shimmerWidth.toPx()
            val travel = length + shimmerPx * 2f
            val centerX = startX - shimmerPx + travel * shimmerPhase.value
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    accent.copy(alpha = 0f),
                    accent.copy(alpha = highlightAlpha),
                    accent.copy(alpha = 0f)
                ),
                start = Offset(centerX - shimmerPx, y),
                end = Offset(centerX + shimmerPx, y)
            )
            drawLine(
                brush = shimmerBrush,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }

    if (drawOnTop) {
        drawWithContent {
            drawContent()
            drawShimmer()
        }
    } else {
        drawBehind(drawShimmer)
    }
}
