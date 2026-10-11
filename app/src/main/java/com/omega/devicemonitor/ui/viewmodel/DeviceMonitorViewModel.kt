package com.omega.devicemonitor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omega.devicemonitor.data.local.DeviceMonitorDatabase
import com.omega.devicemonitor.data.local.entity.AlertLogEntity
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import com.omega.devicemonitor.data.local.entity.ThresholdEntity
import com.omega.devicemonitor.data.local.entity.UserProfileEntity
import com.omega.devicemonitor.data.network.Esp32ApiClient
import com.omega.devicemonitor.data.network.Esp32PollResult
import com.omega.devicemonitor.data.repository.DeviceMonitorRepository
import com.omega.devicemonitor.data.repository.StorageStats
import com.omega.devicemonitor.util.SecurityHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DeviceMonitorDatabase.getDatabase(application)
    private val repository = DeviceMonitorRepository(db)
    private val esp32Client = Esp32ApiClient()

    // Authentication State
    private val _currentUser = MutableStateFlow<UserProfileEntity?>(null)
    val currentUser: StateFlow<UserProfileEntity?> = _currentUser.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Preferences & Filter state
    val selectedCategoryFilter = MutableStateFlow("ALL")
    val tempUnitFahrenheit = MutableStateFlow(false)
    val darkModeOverride = MutableStateFlow<Boolean?>(null)

    // HTTP test state
    private val _httpTestResult = MutableStateFlow<Esp32PollResult?>(null)
    val httpTestResult: StateFlow<Esp32PollResult?> = _httpTestResult.asStateFlow()

    private val _isTestingHttp = MutableStateFlow(false)
    val isTestingHttp: StateFlow<Boolean> = _isTestingHttp.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats(0, 0, 0, 0, 0))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    // Per-User Reactive Streams from Room
    val devices: StateFlow<List<DeviceEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getDevicesForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readings: StateFlow<List<SensorReadingEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getReadingsForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thresholds: StateFlow<List<ThresholdEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getThresholdsForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alerts: StateFlow<List<AlertLogEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getAlertsForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unacknowledgedAlerts: StateFlow<List<AlertLogEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getUnacknowledgedAlertsForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unacknowledgedAlertCount: StateFlow<Int> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getUnacknowledgedCountForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deviceCount: StateFlow<Int> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getDeviceCountForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val readingCount: StateFlow<Int> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getReadingCountForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val thresholdCount: StateFlow<Int> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getThresholdCountForUser(user.id) else emptyFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Authentication Actions
    fun login(email: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = repository.loginUser(email, pass)
            _authLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    tempUnitFahrenheit.value = user.tempUnitFahrenheit
                    refreshStorageStats()
                    _userMessage.emit("Welcome back, ${user.name}!")
                    onSuccess()
                },
                onFailure = { err ->
                    _authError.value = err.localizedMessage ?: "Login failed"
                }
            )
        }
    }

    fun register(name: String, email: String, pass: String, avatarIndex: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = repository.registerUser(name, email, pass, avatarIndex)
            _authLoading.value = false
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    tempUnitFahrenheit.value = user.tempUnitFahrenheit
                    refreshStorageStats()
                    _userMessage.emit("Account created. Welcome to DeviceMonitor!")
                    onSuccess()
                },
                onFailure = { err ->
                    _authError.value = err.localizedMessage ?: "Registration failed"
                }
            )
        }
    }

    fun logout() {
        val name = _currentUser.value?.name ?: "User"
        _currentUser.value = null
        viewModelScope.launch {
            _userMessage.emit("Logged out of $name's account")
        }
    }

    fun setTemperatureUnit(fahrenheit: Boolean) {
        tempUnitFahrenheit.value = fahrenheit
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val updated = user.copy(tempUnitFahrenheit = fahrenheit)
                repository.updateUserProfile(updated)
                _currentUser.value = updated
                val unitName = if (fahrenheit) "Fahrenheit (°F)" else "Celsius (°C)"
                _userMessage.emit("Temperature unit set to $unitName")
            } catch (e: Exception) {
                // Keep local state
            }
        }
    }

    fun updateProfile(name: String, email: String, avatarIndex: Int, newPassword: String? = null) {
        val user = _currentUser.value ?: return
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            viewModelScope.launch {
                _userMessage.emit("Display name cannot be empty")
            }
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            viewModelScope.launch {
                _userMessage.emit("Please enter a valid email address")
            }
            return
        }

        viewModelScope.launch {
            try {
                // If email changed, check for conflict with another account
                if (!cleanEmail.equals(user.email, ignoreCase = true)) {
                    val existing = repository.getUserByEmail(cleanEmail)
                    if (existing != null && existing.id != user.id) {
                        _userMessage.emit("Email '$cleanEmail' is already in use by another account")
                        return@launch
                    }
                }

                val newHash = if (!newPassword.isNullOrBlank()) {
                    if (newPassword.length < 6) {
                        _userMessage.emit("Password must be at least 6 characters")
                        return@launch
                    }
                    SecurityHelper.hashPassword(newPassword)
                } else {
                    user.passwordHash
                }

                val updated = user.copy(
                    name = cleanName,
                    email = cleanEmail,
                    passwordHash = newHash,
                    avatarColorIndex = avatarIndex,
                    tempUnitFahrenheit = tempUnitFahrenheit.value
                )
                repository.updateUserProfile(updated)
                _currentUser.value = updated
                _userMessage.emit("Profile updated successfully")
            } catch (e: Exception) {
                _userMessage.emit("Error updating profile: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun refreshStorageStats() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _storageStats.value = repository.getUserStorageStats(user.id)
        }
    }

    // Devices (All users have equal access!)
    fun addDevice(name: String, location: String, ipAddress: String, deviceType: String, notes: String = "") {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertDevice(
                DeviceEntity(
                    userId = user.id,
                    name = name.trim(),
                    location = location.trim(),
                    ipAddress = ipAddress.trim(),
                    deviceType = deviceType,
                    notes = notes.trim()
                )
            )
            refreshStorageStats()
            _userMessage.emit("Device '$name' added")
        }
    }

    fun updateDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.updateDevice(device)
            refreshStorageStats()
            _userMessage.emit("Device updated")
        }
    }

    fun deleteDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            refreshStorageStats()
            _userMessage.emit("Deleted device '${device.name}'")
        }
    }

    // Readings (Manual Entry vs Hardware)
    fun logManualReading(
        deviceId: Long,
        sensorName: String,
        sensorType: String,
        value: Double,
        unit: String,
        notes: String = ""
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertReading(
                SensorReadingEntity(
                    deviceId = deviceId,
                    userId = user.id,
                    sensorName = sensorName.trim(),
                    sensorType = sensorType,
                    value = value,
                    unit = unit.trim(),
                    isManual = true,
                    notes = notes.trim()
                )
            )
            refreshStorageStats()
            _userMessage.emit("Logged reading for '$sensorName'")
        }
    }

    fun deleteReading(reading: SensorReadingEntity) {
        viewModelScope.launch {
            repository.deleteReading(reading)
            refreshStorageStats()
        }
    }

    // Thresholds
    fun addThreshold(
        deviceId: Long?,
        sensorName: String,
        sensorType: String,
        condition: String,
        minValue: Double?,
        maxValue: Double?,
        severity: String,
        customMessage: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertThreshold(
                ThresholdEntity(
                    userId = user.id,
                    deviceId = deviceId,
                    sensorName = sensorName.trim(),
                    sensorType = sensorType,
                    condition = condition,
                    minValue = minValue,
                    maxValue = maxValue,
                    severity = severity,
                    customMessage = customMessage.trim()
                )
            )
            refreshStorageStats()
            _userMessage.emit("Threshold rule created")
        }
    }

    fun toggleThreshold(threshold: ThresholdEntity) {
        viewModelScope.launch {
            repository.updateThreshold(threshold.copy(enabled = !threshold.enabled))
            refreshStorageStats()
        }
    }

    fun deleteThreshold(threshold: ThresholdEntity) {
        viewModelScope.launch {
            repository.deleteThreshold(threshold)
            refreshStorageStats()
            _userMessage.emit("Threshold rule deleted")
        }
    }

    // Alerts
    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId)
        }
    }

    fun acknowledgeAllAlerts() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.acknowledgeAllAlertsForUser(user.id)
            _userMessage.emit("All alerts acknowledged")
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            repository.deleteAlertById(alertId)
            refreshStorageStats()
        }
    }

    // HTTP / ESP32 Connectivity Layer
    fun testEsp32Connection(ipAddress: String, path: String = "/api/sensors") {
        viewModelScope.launch {
            _isTestingHttp.value = true
            _httpTestResult.value = null
            val result = esp32Client.pollDevice(ipAddress, path)
            _httpTestResult.value = result
            _isTestingHttp.value = false
        }
    }

    fun clearHttpTestResult() {
        _httpTestResult.value = null
    }

    fun commitPolledReadingsToDevice(deviceId: Long, readings: List<com.omega.devicemonitor.data.network.ParsedReading>) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            var inserted = 0
            val now = System.currentTimeMillis()
            readings.forEach { r ->
                repository.insertReading(
                    SensorReadingEntity(
                        deviceId = deviceId,
                        userId = user.id,
                        sensorName = r.sensorName,
                        sensorType = r.sensorType,
                        value = r.value,
                        unit = r.unit,
                        timestamp = now,
                        isManual = false,
                        notes = "Hardware Sync"
                    )
                )
                inserted++
            }
            refreshStorageStats()
            _userMessage.emit("Received $inserted reading(s) from device")
        }
    }

    fun simulateEsp32Packet(deviceId: Long, deviceName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val randomFactor = (System.currentTimeMillis() % 10) / 10.0
            val sampleItems = listOf(
                SensorReadingEntity(userId = user.id, deviceId = deviceId, sensorName = "Core Temp", sensorType = "TEMPERATURE", value = 24.2 + randomFactor * 2.5, unit = "°C", timestamp = now, isManual = true, notes = "Simulated ESP32 packet"),
                SensorReadingEntity(userId = user.id, deviceId = deviceId, sensorName = "Air Humidity", sensorType = "HUMIDITY", value = 54.0 + randomFactor * 4.0, unit = "%", timestamp = now, isManual = true, notes = "Simulated ESP32 packet"),
                SensorReadingEntity(userId = user.id, deviceId = deviceId, sensorName = "Ambient Lux", sensorType = "LIGHT", value = 320.0 + randomFactor * 80.0, unit = "lux", timestamp = now, isManual = true, notes = "Simulated ESP32 packet")
            )
            sampleItems.forEach { repository.insertReading(it) }
            refreshStorageStats()
            _userMessage.emit("Simulated ESP32 readings added for $deviceName")
        }
    }

    // Storage Management (Per-User)
    fun clearSensorHistory() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearSensorHistoryForUser(user.id)
            refreshStorageStats()
            _userMessage.emit("All sensor readings cleared")
        }
    }

    fun clearAlertHistory() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearAlertsForUser(user.id)
            refreshStorageStats()
            _userMessage.emit("All alert history cleared")
        }
    }

    fun resetAllMonitoringData() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.resetUserData(user.id)
            refreshStorageStats()
            _userMessage.emit("Monitoring data reset")
        }
    }

    fun seedStarterData() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.seedStarterDataForUser(user.id)
            refreshStorageStats()
            _userMessage.emit("Sample devices & sensors loaded")
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val json = repository.exportUserDataAsJson(user.id)
            onResult(json)
        }
    }

    fun importBackup(jsonString: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.importUserDataFromJson(user.id, jsonString)
            res.fold(
                onSuccess = { msg ->
                    refreshStorageStats()
                    _userMessage.emit(msg)
                },
                onFailure = { err ->
                    _userMessage.emit(err.localizedMessage ?: "Import failed")
                }
            )
        }
    }
}
