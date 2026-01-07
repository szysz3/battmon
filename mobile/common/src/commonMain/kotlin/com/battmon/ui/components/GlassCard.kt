package com.battmon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isLightSurface) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    } else {
        Color.Transparent
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
