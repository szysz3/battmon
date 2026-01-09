package com.battmon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

// Spacing tokens
@Suppress("unused")
object Spacing {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Card-specific spacing
    val cardPaddingHero = 26.dp
    val cardPaddingLarge = 22.dp
    val cardPaddingMedium = 18.dp
    val cardPaddingSmall = 16.dp
}

// Corner radius tokens
@Suppress("unused")
object CornerRadius {
    val small = 12.dp
    val medium = 16.dp
    val large = 20.dp
    val xl = 22.dp
    val xxl = 24.dp
    val xxxl = 28.dp
}

// Elevation tokens
@Suppress("unused")
object Elevation {
    val none = 0.dp
    val low = 2.dp
    val medium = 6.dp
    val high = 8.dp
    val veryHigh = 10.dp
    val extreme = 12.dp
}

// Gradient constants
private object GradientBlend {
    const val START_BLEND = 0.12f
    const val MID_BLEND_LIGHT = 0.16f
    const val MID_BLEND_DARK = 0.1f
    const val END_BLEND_LIGHT = 0.3f
    const val END_BLEND_DARK = 0.24f
}

@Composable
fun cardGradient(accent: Color): Brush {
    val surface = MaterialTheme.colorScheme.surface
    val variant = MaterialTheme.colorScheme.surfaceVariant
    val isLightSurface = surface.luminance() > 0.5f
    val midBlend = if (isLightSurface) GradientBlend.MID_BLEND_LIGHT else GradientBlend.MID_BLEND_DARK
    val endBlend = if (isLightSurface) GradientBlend.END_BLEND_LIGHT else GradientBlend.END_BLEND_DARK

    return Brush.linearGradient(
        colors = listOf(
            lerp(surface, variant, GradientBlend.START_BLEND),
            lerp(variant, accent, midBlend),
            lerp(surface, variant, endBlend)
        )
    )
}

@Composable
fun filterCardGradient(accentTone: Color): Brush {
    val surfaceTone = MaterialTheme.colorScheme.surface
    val variantTone = MaterialTheme.colorScheme.surfaceVariant
    val isLightSurface = surfaceTone.luminance() > 0.5f

    return if (isLightSurface) {
        Brush.linearGradient(
            colors = listOf(
                lerp(surfaceTone, variantTone, 0.12f),
                lerp(variantTone, accentTone, 0.07f),
                lerp(surfaceTone, variantTone, 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                surfaceTone.copy(alpha = 0.92f),
                variantTone.copy(alpha = 0.85f),
                surfaceTone.copy(alpha = 0.78f)
            )
        )
    }
}

@Composable
fun historyItemGradient(accentTone: Color): Brush {
    val surfaceTone = MaterialTheme.colorScheme.surface
    val variantTone = MaterialTheme.colorScheme.surfaceVariant
    val isLightSurface = surfaceTone.luminance() > 0.5f

    return if (isLightSurface) {
        Brush.linearGradient(
            colors = listOf(
                lerp(surfaceTone, variantTone, 0.15f),
                lerp(variantTone, accentTone, 0.08f),
                lerp(surfaceTone, variantTone, 0.3f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                surfaceTone.copy(alpha = 0.9f),
                variantTone.copy(alpha = 0.86f),
                surfaceTone.copy(alpha = 0.74f)
            )
        )
    }
}
