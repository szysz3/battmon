package com.battmon.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Home)
    data object History : Screen("history", "History", Icons.AutoMirrored.Filled.List)

    companion object {
        val items: List<Screen>
            get() = listOf(Dashboard, History)
    }
}
