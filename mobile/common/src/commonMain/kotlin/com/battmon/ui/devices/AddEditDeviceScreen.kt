package com.battmon.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.battmon.ui.components.GlassCard
import com.battmon.ui.components.Label
import com.battmon.ui.components.LabelVariant
import com.battmon.ui.components.glassAccentShimmer
import com.battmon.ui.components.styledCard
import com.battmon.ui.state.UiState
import com.battmon.ui.theme.filterCardGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeviceScreen(
    deviceId: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: AddEditDeviceViewModel = viewModel { AddEditDeviceViewModel(deviceId) }
) {
    val name by viewModel.name.collectAsState()
    val host by viewModel.host.collectAsState()
    val port by viewModel.port.collectAsState()

    val nameError by viewModel.nameError.collectAsState()
    val hostError by viewModel.hostError.collectAsState()
    val portError by viewModel.portError.collectAsState()

    val saveState by viewModel.saveState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) {
            viewModel.resetSaveState()
            onSaveSuccess()
        }
    }

    LaunchedEffect(deviceId) {
        viewModel.resetForm()
    }

    Dialog(onDismissRequest = onNavigateBack) {
        val accentTone = MaterialTheme.colorScheme.primary
        val cardShape = RoundedCornerShape(24.dp)

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .styledCard(cardShape)
                .glassAccentShimmer(accentTone),
            gradient = filterCardGradient(accentTone),
            elevation = 2.dp,
            cornerRadius = 24.dp,
            padding = 20.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewModel.isEditMode) "Edit Device" else "Add Device",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Label(text = "DEVICE", variant = LabelVariant.CARD)
                    Spacer(modifier = Modifier.height(0.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Name") },
                        placeholder = { Text("e.g., Office UPS") },
                        supportingText = {
                            nameError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("Shown in the master selector")
                        },
                        isError = nameError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Label(text = "CONNECTION", variant = LabelVariant.CARD)
                    Spacer(modifier = Modifier.height(0.dp))
                    OutlinedTextField(
                        value = host,
                        onValueChange = { viewModel.updateHost(it) },
                        label = { Text("Host") },
                        placeholder = { Text("e.g., 192.168.1.100") },
                        supportingText = {
                            hostError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("IP address or hostname of apcupsd server")
                        },
                        isError = hostError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = port,
                        onValueChange = { viewModel.updatePort(it) },
                        label = { Text("Port") },
                        placeholder = { Text("3551") },
                        supportingText = {
                            portError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            } ?: Text("apcupsd NIS port (default: 3551)")
                        },
                        isError = portError != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (saveState is UiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                val errorShape = RoundedCornerShape(18.dp)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .styledCard(errorShape),
                    gradient = filterCardGradient(MaterialTheme.colorScheme.error),
                    elevation = 2.dp,
                    cornerRadius = 18.dp,
                    padding = 16.dp
                ) {
                    Text(
                        text = (saveState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save() },
                enabled = saveState !is UiState.Loading && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (saveState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Save")
            }
        }
    }
}
