package com.battmon.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Status Colors
val StatusOnline = Color(0xFF10B981)        // Emerald Green
val StatusOnBattery = Color(0xFFEF4444)     // Vibrant Red
val StatusWarning = Color(0xFFF59E0B)       // Amber
val StatusOffline = Color(0xFF6B7280)       // Slate Gray

// Primary Colors
val PrimaryBlue = Color(0xFF3B82F6)         // Modern Blue
val SecondaryTeal = Color(0xFF14B8A6)       // Teal
val AccentPurple = Color(0xFF8B5CF6)        // Purple
val AccentPink = Color(0xFFEC4899)          // Pink

// Background Colors
val SurfaceLight = Color(0xFFF8FAFC)        // Soft white
val SurfaceDark = Color(0xFF0F172A)         // Deep dark blue
val GlassLight = Color(0xE6FFFFFF)          // Frosted white
val GlassDark = Color(0xE61E293B)           // Frosted dark blue

// Modern iOS-inspired Gradients with multiple colors
val StatusGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2E5FAE),  // Deep blue
        Color(0xFF3A76C8),  // Mid blue
        Color(0xFF3E8F8A)   // Deep teal
    )
)

val BatteryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2FBF9B),  // Soft emerald
        Color(0xFF1AA07A),  // Deep emerald
        Color(0xFF6AD9B6)   // Light emerald
    )
)

val LoadGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF4F8FE6),  // Soft blue
        Color(0xFF3576D2),  // Mid blue
        Color(0xFF8DBAF2)   // Light blue
    )
)

val TimeGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFF2C77A),  // Soft amber
        Color(0xFFE3A85A),  // Warm amber
        Color(0xFFF7D7A3)   // Light amber
    )
)

val VoltageGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF6FA3FF),  // Soft blue
        Color(0xFF4E86E6),  // Mid blue
        Color(0xFFA7C5FF)   // Light blue
    )
)

val TransferGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF32B7B3),  // Soft teal
        Color(0xFF189D99),  // Deep teal
        Color(0xFF7AD9D4)   // Light teal
    )
)
