package com.battmon.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class LabelVariant {
    CARD,
    SECTION
}

@Composable
fun Label(
    text: String,
    variant: LabelVariant = LabelVariant.CARD,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val defaultColor = color ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    val bottomPadding = if (variant == LabelVariant.SECTION) 8.dp else 0.dp

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = defaultColor,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        modifier = modifier.padding(bottom = bottomPadding)
    )
}
