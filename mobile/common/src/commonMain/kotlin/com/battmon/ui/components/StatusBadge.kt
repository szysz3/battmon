package com.battmon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.battmon.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val normalized = status.uppercase()
    val (color, text) = when {
        normalized.contains("ONLINE") -> StatusOnline to "Online"
        normalized.contains("ONBATT") -> StatusOnBattery to "On Battery"
        normalized.contains("LOWBATT") -> StatusOnBattery to "Low Battery"
        normalized.contains("COMMLOST") -> StatusOffline to "Comm Lost"
        else -> StatusWarning to status
    }

    val fontSize = if (large) 17.sp else 13.sp
    val horizontalPadding = if (large) 20.dp else 12.dp
    val verticalPadding = if (large) 10.dp else 6.dp
    val cornerRadius = if (large) 16.dp else 12.dp
    val fontWeight = if (large) FontWeight.SemiBold else FontWeight.Medium
    val textColor = if (large) Color.White else color
    val backgroundAlpha = if (large) 0.35f else 0.18f

    Box(
        modifier = modifier
            .background(color.copy(alpha = backgroundAlpha), RoundedCornerShape(cornerRadius))
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}
