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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.battmon.ui.devices.AddEditDeviceScreen
import com.battmon.ui.devices.DeviceDetailScreen
import com.battmon.ui.devices.DeviceSelectionViewModel
import com.battmon.ui.devices.EmptyDeviceList
import com.battmon.ui.history.HistoryScreen
import com.battmon.ui.navigation.Screen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Power
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.UpsDevice
import com.battmon.ui.state.UiState

@Composable
fun BattmonApp() {
    val selectionViewModel: DeviceSelectionViewModel = viewModel { DeviceSelectionViewModel() }
    val devicesState by selectionViewModel.devices.collectAsState()
    val selectedDeviceId by selectionViewModel.selectedDeviceId.collectAsState()
    val deleteState by selectionViewModel.deleteState.collectAsState()

    var currentRoute by rememberSaveable { mutableStateOf(Screen.Devices.route) }
    var previousRoute by rememberSaveable { mutableStateOf(Screen.Devices.route) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showAddDeviceModal by rememberSaveable { mutableStateOf(false) }

    val bottomNavScreens = remember { Screen.bottomNavItems }
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.background
        )
    )

    // Navigation helper
    fun navigate(route: String) {
        if (route != currentRoute) {
            previousRoute = currentRoute
            currentRoute = route
        }
    }

    fun navigateBack() {
        // Simple back navigation - go to devices list if on a detail screen
        when {
            currentRoute.startsWith("device/") -> navigate(Screen.Devices.route)
            else -> navigate(Screen.Devices.route)
        }
    }

    // Check if current route is a bottom nav route
    val isBottomNavRoute = bottomNavScreens.any { it.route == currentRoute }

    LaunchedEffect(deleteState) {
        if (deleteState is UiState.Success) {
            selectionViewModel.resetDeleteState()
            showDeleteDialog = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (isBottomNavRoute) {
                    DeviceMasterTopBar(
                        devicesState = devicesState,
                        selectedDeviceId = selectedDeviceId,
                        onSelectDevice = selectionViewModel::selectDevice,
                        onAddDevice = { showAddDeviceModal = true },
                        onDeleteDevice = { showDeleteDialog = true }
                    )
                }
            },
            bottomBar = {
                // Only show bottom bar for main navigation screens
                if (isBottomNavRoute) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onSelect = { screen ->
                            navigate(screen.route)
                        }
                    )
                }
            }
        ) { paddingValues ->
            val currentIndex = bottomNavScreens.indexOfFirst { it.route == currentRoute }
            val previousIndex = bottomNavScreens.indexOfFirst { it.route == previousRoute }
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
                modifier = Modifier.padding(paddingValues),
                label = "screen_transition"
            ) { route ->
                when {
                    // Device Detail (Home)
                    route == Screen.Devices.route -> {
                        DeviceHomeContent(
                            devicesState = devicesState,
                            selectedDeviceId = selectedDeviceId,
                            onAddDevice = { showAddDeviceModal = true }
                        )
                    }

                    // History
                    route == Screen.History.route -> {
                        HistoryScreen(
                            selectedDeviceId = selectedDeviceId
                        )
                    }

                    // Device Detail
                    // Default - Device List
                    else -> {
                        DeviceHomeContent(
                            devicesState = devicesState,
                            selectedDeviceId = selectedDeviceId,
                            onAddDevice = { showAddDeviceModal = true }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        val selectedDevice = (devicesState as? UiState.Success)
            ?.data
            ?.firstOrNull { it.id == selectedDeviceId }
        val dialogShape = RoundedCornerShape(22.dp)
        val dialogSurface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        val dialogTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Device?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${selectedDevice?.name ?: "this device"}? This will also delete all status history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = dialogTextColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { selectionViewModel.deleteSelectedDevice() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = dialogShape,
            containerColor = dialogSurface,
            tonalElevation = 2.dp
        )
    }

    if (showAddDeviceModal) {
        AddEditDeviceScreen(
            deviceId = null,
            onNavigateBack = { showAddDeviceModal = false },
            onSaveSuccess = {
                selectionViewModel.refreshDevices()
                showAddDeviceModal = false
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DeviceMasterTopBar(
    devicesState: UiState<List<UpsDevice>>,
    selectedDeviceId: String?,
    onSelectDevice: (String?) -> Unit,
    onAddDevice: () -> Unit,
    onDeleteDevice: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val devices = (devicesState as? UiState.Success)?.data.orEmpty()
    val selectedDeviceName = devices.firstOrNull { it.id == selectedDeviceId }?.name ?: "Select UPS"
    val topBarGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarGradient)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE UPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current
                                ) { if (devices.isNotEmpty()) expanded = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDeviceName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select UPS",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.widthIn(min = 220.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 2.dp
                        ) {
                            devices.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device.name) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Power,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        onSelectDevice(device.id)
                                        expanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface,
                                        leadingIconColor = MaterialTheme.colorScheme.onSurface,
                                        trailingIconColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onAddDevice) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add device")
                    }
                    IconButton(onClick = onDeleteDevice, enabled = selectedDeviceId != null) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete device")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceHomeContent(
    devicesState: UiState<List<UpsDevice>>,
    selectedDeviceId: String?,
    onAddDevice: () -> Unit
) {
    when (devicesState) {
        is UiState.Success -> {
            val deviceId = selectedDeviceId
            if (deviceId != null) {
                DeviceDetailScreen(
                    deviceId = deviceId,
                    showTopBar = false
                )
            } else {
                EmptyDeviceList(onAddDevice = onAddDevice)
            }
        }
        is UiState.Loading, is UiState.Initial -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            EmptyDeviceList(onAddDevice = onAddDevice)
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String,
    onSelect: (Screen) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    val bottomBarGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .offset(y = (-16).dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.10f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bottomBarGradient)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )

            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Screen.bottomNavItems.forEach { screen ->
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
    }
}
