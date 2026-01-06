package com.battmon.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    object History : Screen("history", "History", Icons.Rounded.History)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)

    companion object {
        val items = listOf(Dashboard, History, Settings)
    }
}
