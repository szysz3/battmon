package com.battmon.ui.devices

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.model.DeviceWithStatus
import com.battmon.ui.components.ErrorView
import com.battmon.ui.components.LoadingShimmer
import com.battmon.ui.state.UiState

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DeviceListScreen(
    onDeviceClick: (String) -> Unit,
    onAddDevice: () -> Unit,
    viewModel: DeviceListViewModel = viewModel { DeviceListViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        AnimatedContent(
            targetState = uiState,
            label = "device_list_content"
        ) { state ->
            when (state) {
                is UiState.Initial,
                is UiState.Loading -> {
                    DeviceListLoading()
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyDeviceList(onAddDevice = onAddDevice)
                    } else {
                        DeviceListContent(
                            devices = state.data,
                            onDeviceClick = onDeviceClick,
                            onDeleteDevice = { viewModel.deleteDevice(it) }
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
            }
        }

        // FAB for adding device
        if (uiState is UiState.Success) {
            FloatingActionButton(
                onClick = onAddDevice,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Device"
                )
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun DeviceListContent(
    devices: List<DeviceWithStatus>,
    onDeviceClick: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = devices,
            key = { it.device.id }
        ) { deviceWithStatus ->
            DeviceCard(
                deviceWithStatus = deviceWithStatus,
                onClick = { onDeviceClick(deviceWithStatus.device.id) }
            )
        }

        // Bottom padding for FAB
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { deviceId ->
        val device = devices.find { it.device.id == deviceId }?.device
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Device?") },
            text = {
                Text("Are you sure you want to delete ${device?.name ?: deviceId}? This will also delete all status history for this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDevice(deviceId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceListLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            )
        }
    }
}
