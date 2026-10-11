package com.omega.devicemonitor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.network.Esp32PollResult
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDeviceDialog(
    initialDevice: DeviceEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, ip: String, type: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(initialDevice?.name ?: "") }
    var location by remember { mutableStateOf(initialDevice?.location ?: "") }
    var ipAddress by remember { mutableStateOf(initialDevice?.ipAddress ?: "192.168.1.") }
    var selectedType by remember { mutableStateOf(initialDevice?.deviceType ?: "ESP32") }
    var notes by remember { mutableStateOf(initialDevice?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    val deviceTypes = listOf("ESP32", "ESP8266", "Raspberry Pi Pico", "Arduino", "Generic MCU")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialDevice == null) "Add IoT Device" else "Edit Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Device Name") },
                    placeholder = { Text("e.g., Living Room Node") },
                    isError = showError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_name_input")
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Zone") },
                    placeholder = { Text("e.g., Balcony, Greenhouse") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_location_input")
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Address / Hostname") },
                    placeholder = { Text("e.g., 192.168.1.120") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_ip_input")
                )

                Text(
                    text = "Hardware Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    deviceTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g., Pin D4 connected to DHT22") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "Device name is required.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(name, location, ipAddress, selectedType, notes)
                    }
                },
                modifier = Modifier.testTag("save_device_button")
            ) {
                Text(if (initialDevice == null) "Add Device" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogReadingDialog(
    devices: List<DeviceEntity>,
    preselectedDeviceId: Long? = null,
    preselectedSensorName: String? = null,
    preselectedSensorType: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (deviceId: Long, name: String, type: String, value: Double, unit: String, notes: String) -> Unit
) {
    var selectedDeviceId by remember {
        mutableStateOf(preselectedDeviceId ?: devices.firstOrNull()?.id ?: 0L)
    }
    var sensorName by remember { mutableStateOf(preselectedSensorName ?: "Temperature") }
    var sensorType by remember { mutableStateOf(preselectedSensorType ?: "TEMPERATURE") }
    var valueString by remember { mutableStateOf("") }
    var unit by remember {
        mutableStateOf(SensorTypeHelper.getDefaultUnit(sensorType))
    }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Manual Reading",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ℹ️ Manual Entry: Clearly tagged to distinguish personal observations from automated ESP32 hardware telemetry.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Device selector
                Text(
                    text = "Target Device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    devices.forEach { dev ->
                        FilterChip(
                            selected = selectedDeviceId == dev.id,
                            onClick = { selectedDeviceId = dev.id },
                            label = { Text(dev.name) }
                        )
                    }
                }

                // Sensor category picker (Equal Priority)
                Text(
                    text = "Sensor Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SensorTypeHelper.ALL_TYPES.forEach { (typeKey, label) ->
                        FilterChip(
                            selected = sensorType == typeKey,
                            onClick = {
                                sensorType = typeKey
                                unit = SensorTypeHelper.getDefaultUnit(typeKey)
                                if (sensorName.isBlank() || sensorName in SensorTypeHelper.ALL_TYPES.map { it.second }) {
                                    sensorName = label
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sensorName,
                    onValueChange = { sensorName = it },
                    label = { Text("Sensor Identifier / Name") },
                    placeholder = { Text("e.g., Soil Moisture Sensor 1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sensor_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = valueString,
                        onValueChange = { valueString = it; showError = false },
                        label = { Text("Numeric Value") },
                        placeholder = { Text("e.g., 23.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = showError && valueString.toDoubleOrNull() == null,
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("sensor_value_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("e.g., °C, %") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("sensor_unit_input")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Verification Note (Optional)") },
                    placeholder = { Text("e.g., Calibrated with lab meter") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "Please enter a valid numeric value.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedVal = valueString.toDoubleOrNull()
                    if (parsedVal == null || selectedDeviceId == 0L) {
                        showError = true
                    } else {
                        onConfirm(
                            selectedDeviceId,
                            sensorName,
                            sensorType,
                            parsedVal,
                            unit,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("submit_reading_button")
            ) {
                Text("Save Reading")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddThresholdDialog(
    devices: List<DeviceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        deviceId: Long?,
        sensorName: String,
        sensorType: String,
        condition: String,
        minValue: Double?,
        maxValue: Double?,
        severity: String,
        customMessage: String
    ) -> Unit
) {
    var selectedDeviceId by remember { mutableStateOf<Long?>(null) } // null = all devices
    var sensorType by remember { mutableStateOf("TEMPERATURE") }
    var sensorName by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("ABOVE") }
    var minValueStr by remember { mutableStateOf("") }
    var maxValueStr by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("WARNING") }
    var customMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Alert Threshold Rule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Applies To Sensor Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SensorTypeHelper.ALL_TYPES.forEach { (typeKey, label) ->
                        FilterChip(
                            selected = sensorType == typeKey,
                            onClick = { sensorType = typeKey },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = sensorName,
                    onValueChange = { sensorName = it },
                    label = { Text("Sensor Name Match (Leave empty for all)") },
                    placeholder = { Text("e.g., Soil Moisture") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Condition Trigger",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ABOVE", "BELOW", "OUT_OF_RANGE").forEach { cond ->
                        FilterChip(
                            selected = condition == cond,
                            onClick = { condition = cond },
                            label = { Text(cond.replace('_', ' ')) }
                        )
                    }
                }

                if (condition == "ABOVE" || condition == "OUT_OF_RANGE") {
                    OutlinedTextField(
                        value = maxValueStr,
                        onValueChange = { maxValueStr = it; showError = false },
                        label = { Text(if (condition == "ABOVE") "Trigger When Above" else "Upper Bound Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (condition == "BELOW" || condition == "OUT_OF_RANGE") {
                    OutlinedTextField(
                        value = minValueStr,
                        onValueChange = { minValueStr = it; showError = false },
                        label = { Text(if (condition == "BELOW") "Trigger When Below" else "Lower Bound Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Severity Level",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("INFO", "WARNING", "CRITICAL").forEach { sev ->
                        FilterChip(
                            selected = severity == sev,
                            onClick = { severity = sev },
                            label = { Text(sev) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Custom Alert Message") },
                    placeholder = { Text("e.g., Soil moisture is critically low!") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text = "Please enter valid numeric limit values.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minValueStr.toDoubleOrNull()
                    val max = maxValueStr.toDoubleOrNull()
                    val valid = when (condition) {
                        "ABOVE" -> max != null
                        "BELOW" -> min != null
                        "OUT_OF_RANGE" -> min != null && max != null
                        else -> false
                    }
                    if (!valid) {
                        showError = true
                    } else {
                        onConfirm(
                            selectedDeviceId,
                            sensorName,
                            sensorType,
                            condition,
                            min,
                            max,
                            severity,
                            customMessage
                        )
                    }
                },
                modifier = Modifier.testTag("save_threshold_button")
            ) {
                Text("Create Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TestHttpDialog(
    device: DeviceEntity,
    isLoading: Boolean,
    testResult: Esp32PollResult?,
    onDismiss: () -> Unit,
    onTestPing: (ip: String, path: String) -> Unit,
    onImportReadings: (List<com.omega.devicemonitor.data.network.ParsedReading>) -> Unit,
    onSimulatePacket: () -> Unit
) {
    var ipInput by remember { mutableStateOf(device.ipAddress) }
    var pathInput by remember { mutableStateOf("/api/sensors") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Connection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Check connection with ${device.name}:",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("Network Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onTestPing(ipInput, pathInput) },
                        enabled = !isLoading && ipInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connecting...")
                        } else {
                            Text("Check Status")
                        }
                    }

                    OutlinedButton(
                        onClick = onSimulatePacket,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simulate")
                    }
                }

                testResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (res.success) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (res.success) "Connected • ${res.responseTimeMs}ms response"
                                else "Unable to connect",
                                fontWeight = FontWeight.Bold,
                                color = if (res.success) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (res.success) "Received current sensor readings."
                                else "Make sure device is powered on and connected to your local network.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (res.readings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Detected Sensors:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                res.readings.forEach { r ->
                                    Text(
                                        text = "• ${r.sensorName}: ${r.value} ${r.unit}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (testResult != null && testResult.readings.isNotEmpty()) {
                Button(onClick = { onImportReadings(testResult.readings) }) {
                    Text("Save Readings")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (testResult != null && testResult.readings.isNotEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Confirm",
    isDestructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isDestructive) Icons.Default.Warning else Icons.Default.Check,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (isDestructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors(),
                modifier = Modifier.testTag("confirm_action_button")
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_action_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
