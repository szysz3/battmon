package com.battmon.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Status Colors
val StatusOnline = Color(0xFF10B981)        // Emerald Green
val StatusOnBattery = Color(0xFFEF4444)     // Vibrant Red
val StatusWarning = Color(0xFFF59E0B)       // Amber
val StatusOffline = Color(0xFF6B7280)       // Slate Gray

// Primary Colors
val PrimaryBlue = Color(0xFF1E3A8A)         // Deep Indigo
val SecondaryTeal = Color(0xFF0F766E)       // Deep Teal
val AccentPurple = Color(0xFF8B5CF6)        // Purple
val AccentPink = Color(0xFFEC4899)          // Pink

// Background Colors
val SurfaceLight = Color(0xFFF5F7FB)        // Soft white
val SurfaceDark = Color(0xFF0B1220)         // Deep dark blue
val GlassLight = Color(0xEFFFFFFF)          // Frosted white
val GlassDark = Color(0xE6121A2B)           // Frosted dark blue

// Modern iOS-inspired Gradients with multiple colors
val StatusGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A2D6F),  // Deep indigo
        Color(0xFF24479A),  // Muted blue
        Color(0xFF1E6B66)   // Deep teal
    )
)

val BatteryGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0F6B5B),  // Deep emerald
        Color(0xFF1C8B78),  // Muted emerald
        Color(0xFF3BA48E)   // Soft emerald
    )
)

val LoadGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1D3E7A),  // Deep blue
        Color(0xFF2A5B9E),  // Mid blue
        Color(0xFF4D7CB9)   // Soft blue
    )
)

val TimeGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF9A6A26),  // Deep amber
        Color(0xFFC0843B),  // Warm amber
        Color(0xFFD3A76D)   // Soft amber
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
