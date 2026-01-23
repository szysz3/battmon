package com.battmon.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.battmon.data.repository.UpsRepository
import com.battmon.model.CreateDeviceRequest
import com.battmon.model.UpdateDeviceRequest
import com.battmon.model.UpsDevice
import com.battmon.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditDeviceViewModel(
    private val editDeviceId: String? = null,
    private val repository: UpsRepository = UpsRepository()
) : ViewModel() {

    companion object {
        /** Maximum length for the sanitized name portion of device ID */
        private const val MAX_ID_NAME_LENGTH = 40

        /** Maximum total length for device ID (must match UpsDevice.ID_PATTERN) */
        private const val MAX_ID_TOTAL_LENGTH = 50

        /** Length of UUID suffix for uniqueness */
        private const val UUID_SUFFIX_LENGTH = 8

        /** Default device ID prefix when name is blank */
        private const val DEFAULT_ID_PREFIX = "ups"
    }

    val isEditMode: Boolean = editDeviceId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _host = MutableStateFlow("")
    val host: StateFlow<String> = _host.asStateFlow()

    private val _port = MutableStateFlow("3551")
    val port: StateFlow<String> = _port.asStateFlow()

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _hostError = MutableStateFlow<String?>(null)
    val hostError: StateFlow<String?> = _hostError.asStateFlow()

    private val _portError = MutableStateFlow<String?>(null)
    val portError: StateFlow<String?> = _portError.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<UpsDevice>>(UiState.Initial)
    val saveState: StateFlow<UiState<UpsDevice>> = _saveState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        if (isEditMode) {
            loadDevice()
        } else {
            resetForm()
        }
    }

    fun resetForm() {
        _name.value = ""
        _host.value = ""
        _port.value = "3551"
        _enabled.value = true
        _nameError.value = null
        _hostError.value = null
        _portError.value = null
        _saveState.value = UiState.Initial
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getDevice(editDeviceId!!)) {
                is UiState.Success -> {
                    val device = result.data
                    _name.value = device.name
                    _host.value = device.host
                    _port.value = device.port.toString()
                    _enabled.value = device.enabled
                }
                is UiState.Error -> {
                    _saveState.value = UiState.Error(result.message)
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun updateName(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun updateHost(value: String) {
        _host.value = value
        _hostError.value = null
    }

    fun updatePort(value: String) {
        _port.value = value.filter { it.isDigit() }
        _portError.value = null
    }

    fun updateEnabled(value: Boolean) {
        _enabled.value = value
    }

    fun save() {
        if (!validate()) return

        viewModelScope.launch {
            _saveState.value = UiState.Loading

            val portInt = _port.value.toIntOrNull() ?: 3551

            _saveState.value = if (isEditMode) {
                val request = UpdateDeviceRequest(
                    name = _name.value,
                    host = _host.value,
                    port = portInt,
                    enabled = _enabled.value
                )
                repository.updateDevice(editDeviceId!!, request)
            } else {
                val generatedId = generateDeviceId(_name.value)
                val request = CreateDeviceRequest(
                    id = generatedId,
                    name = _name.value,
                    host = _host.value,
                    port = portInt,
                    enabled = _enabled.value
                )
                repository.createDevice(request)
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true

        // Name validation
        if (_name.value.isBlank()) {
            _nameError.value = "Name is required"
            isValid = false
        } else if (_name.value.length > 100) {
            _nameError.value = "Name must be at most 100 characters"
            isValid = false
        }

        // Host and port validation
        if (!validateHostAndPort()) {
            isValid = false
        }

        return isValid
    }

    private fun validateHostAndPort(): Boolean {
        var isValid = true

        if (_host.value.isBlank()) {
            _hostError.value = "Host is required"
            isValid = false
        }

        val portInt = _port.value.toIntOrNull()
        if (portInt == null || portInt < 1 || portInt > 65535) {
            _portError.value = "Port must be 1-65535"
            isValid = false
        }

        return isValid
    }

    fun resetSaveState() {
        _saveState.value = UiState.Initial
    }

    /**
     * Generates a unique device ID from the device name.
     * Format: {sanitized-name}-{uuid-suffix}
     * Uses UUID for guaranteed uniqueness.
     *
     * The ID is constrained to [MAX_ID_TOTAL_LENGTH] characters total,
     * with the name portion limited to [MAX_ID_NAME_LENGTH] characters.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(name: String): String {
        val base = name
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { DEFAULT_ID_PREFIX }

        val uuidSuffix = Uuid.random().toString().take(UUID_SUFFIX_LENGTH)
        val trimmed = base.take(MAX_ID_NAME_LENGTH)
        val candidate = "$trimmed-$uuidSuffix"
        return candidate.take(MAX_ID_TOTAL_LENGTH).trim('-')
    }
}
