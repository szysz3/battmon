package com.battmon.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies common card styling: shadow and clip
 * Note: Apply glassAccentShimmer separately after this if shimmer is needed
 */
fun Modifier.styledCard(
    shape: Shape,
    shadowElevation: Dp = 10.dp
): Modifier = this
    .shadow(shadowElevation, shape, clip = false)
    .clip(shape)
