package com.battmon.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    // Bottom navigation tabs
    data object Devices : Screen("devices", "Devices", Icons.Filled.Home)
    data object History : Screen("history", "History", Icons.AutoMirrored.Filled.List)

    // Device management screens (not in bottom nav)
    data object DeviceDetail : Screen("device/{deviceId}", "Device", Icons.Filled.Home) {
        fun createRoute(deviceId: String) = "device/$deviceId"
    }
    data object AddDevice : Screen("device/add", "Add Device", Icons.Filled.Home)

    companion object {
        /** Bottom navigation items */
        val bottomNavItems: List<Screen>
            get() = listOf(Devices, History)
    }
}
