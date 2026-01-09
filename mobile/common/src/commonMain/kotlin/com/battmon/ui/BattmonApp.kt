package com.battmon.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.battmon.ui.dashboard.DashboardScreen
import com.battmon.ui.history.HistoryScreen
import com.battmon.ui.navigation.Screen

@Composable
fun BattmonApp() {
    var currentRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    var previousRoute by rememberSaveable { mutableStateOf(Screen.Dashboard.route) }
    val screens = remember { Screen.items }
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onSelect = { screen ->
                        if (screen.route != currentRoute) {
                            previousRoute = currentRoute
                            currentRoute = screen.route
                        }
                    }
                )
            }
        ) { paddingValues ->
            val currentIndex = screens.indexOfFirst { it.route == currentRoute }
            val previousIndex = screens.indexOfFirst { it.route == previousRoute }
            val direction = if (currentIndex >= previousIndex) 1 else -1
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 280),
                        initialOffsetX = { fullWidth -> fullWidth * direction }
                    ) + fadeIn(animationSpec = tween(durationMillis = 140)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 240),
                            targetOffsetX = { fullWidth -> -fullWidth * direction }
                        ) + fadeOut(animationSpec = tween(durationMillis = 120))
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                when (it) {
                    Screen.Dashboard.route -> DashboardScreen()
                    Screen.History.route -> HistoryScreen()
                    else -> DashboardScreen()
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onSelect: (Screen) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Screen.items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = { onSelect(screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accentColor,
                    selectedTextColor = accentColor,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = accentColor.copy(alpha = 0.14f)
                )
            )
        }
    }
}
