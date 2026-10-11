package com.omega.devicemonitor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import com.omega.devicemonitor.ui.components.AddDeviceDialog
import com.omega.devicemonitor.ui.components.LogReadingDialog
import com.omega.devicemonitor.ui.components.SensorBadge
import com.omega.devicemonitor.ui.components.SensorTypeHelper
import com.omega.devicemonitor.ui.viewmodel.DeviceMonitorViewModel

@Composable
fun DashboardScreen(
    viewModel: DeviceMonitorViewModel,
    onNavigateToSensorDetail: (deviceId: Long, sensorName: String, sensorType: String) -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val unacknowledgedAlertCount by viewModel.unacknowledgedAlertCount.collectAsStateWithLifecycle()
    val unacknowledgedAlerts by viewModel.unacknowledgedAlerts.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isFahrenheit by viewModel.tempUnitFahrenheit.collectAsStateWithLifecycle()

    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showLogReadingDialog by remember { mutableStateOf(false) }

    // Group readings by (deviceId, sensorName) to find latest reading for each sensor
    val latestSensorMap = remember(readings, devices) {
        val deviceNameMap = devices.associate { it.id to it.name }
        val map = mutableMapOf<String, SensorReadingEntity>()
        // Since readings is sorted timestamp DESC, the first occurrence of key is the latest
        readings.forEach { r ->
            val key = "${r.deviceId}_${r.sensorName}"
            if (!map.containsKey(key)) {
                map[key] = r
            }
        }
        map.values.toList().map { r ->
            val devName = deviceNameMap[r.deviceId] ?: "Device #${r.deviceId}"
            r to devName
        }
    }

    // Filter sensor cards by selected category
    val filteredSensors = remember(latestSensorMap, selectedCategory) {
        if (selectedCategory == "ALL") {
            latestSensorMap
        } else {
            latestSensorMap.filter { it.first.sensorType.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val onlineDeviceCount = remember(devices) {
        devices.count { it.status.equals("ONLINE", ignoreCase = true) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title & Status
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (unacknowledgedAlertCount > 0) {
                        Surface(
                            onClick = onNavigateToAlerts,
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.testTag("dashboard_alert_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$unacknowledgedAlertCount Alerts",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Quick Status Summary Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "DEVICES",
                        value = "${devices.size}",
                        subtitle = "$onlineDeviceCount Online",
                        icon = Icons.Default.Devices,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToDevices() }
                    )
                    MetricCard(
                        title = "TELEMETRY",
                        value = "${readings.size}",
                        subtitle = "${latestSensorMap.size} Active",
                        icon = Icons.Default.Sensors,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Alerts Alert Banner (if any)
            if (unacknowledgedAlerts.isNotEmpty()) {
                item {
                    val firstAlert = unacknowledgedAlerts.first()
                    Card(
                        onClick = onNavigateToAlerts,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("active_alert_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${firstAlert.severity}: ${firstAlert.sensorName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = firstAlert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Quick Category Filters
            item {
                Column {
                    Text(
                        text = "Sensors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = { viewModel.selectedCategoryFilter.value = "ALL" },
                                label = { Text("All (${latestSensorMap.size})") },
                                modifier = Modifier.testTag("filter_category_all")
                            )
                        }
                        items(SensorTypeHelper.ALL_TYPES) { (typeKey, label) ->
                            val count = latestSensorMap.count { it.first.sensorType.equals(typeKey, ignoreCase = true) }
                            FilterChip(
                                selected = selectedCategory == typeKey,
                                onClick = { viewModel.selectedCategoryFilter.value = typeKey },
                                label = { Text("$label ($count)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = SensorTypeHelper.getIcon(typeKey),
                                        contentDescription = null,
                                        tint = SensorTypeHelper.getColor(typeKey),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("filter_category_$typeKey")
                            )
                        }
                    }
                }
            }

            // Sensor Readings List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Readings (${filteredSensors.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { showLogReadingDialog = true },
                        modifier = Modifier.testTag("quick_log_reading_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Reading")
                    }
                }
            }

            // Empty state
            if (filteredSensors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No readings available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Log a reading or sample a device to populate telemetry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(onClick = { showLogReadingDialog = true }) {
                                Text("Log Reading")
                            }
                        }
                    }
                }
            }

            // Sensor Telemetry Cards
            items(filteredSensors) { (reading, devName) ->
                val (formattedVal, displayUnit) = SensorTypeHelper.formatValue(
                    reading.value,
                    reading.unit,
                    isFahrenheit,
                    reading.sensorType
                )
                val relativeTime = SensorTypeHelper.formatRelativeTime(reading.timestamp)
                val sensorColor = SensorTypeHelper.getColor(reading.sensorType)

                ElevatedCard(
                    onClick = {
                        onNavigateToSensorDetail(
                            reading.deviceId,
                            reading.sensorName,
                            reading.sensorType
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .testTag("sensor_card_${reading.sensorName}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SensorBadge(
                            sensorType = reading.sensorType,
                            size = 46.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = reading.sensorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "$devName • $relativeTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formattedVal,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = sensorColor
                            )
                            Text(
                                text = displayUnit,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                if (devices.isEmpty()) {
                    showAddDeviceDialog = true
                } else {
                    showLogReadingDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("dashboard_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reading")
        }
    }

    // Dialogs
    if (showAddDeviceDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDeviceDialog = false },
            onConfirm = { name, location, ip, type, notes ->
                viewModel.addDevice(name, location, ip, type, notes)
                showAddDeviceDialog = false
            }
        )
    }

    if (showLogReadingDialog) {
        if (devices.isEmpty()) {
            AddDeviceDialog(
                onDismiss = { showLogReadingDialog = false },
                onConfirm = { name, location, ip, type, notes ->
                    viewModel.addDevice(name, location, ip, type, notes)
                    showLogReadingDialog = false
                }
            )
        } else {
            LogReadingDialog(
                devices = devices,
                onDismiss = { showLogReadingDialog = false },
                onConfirm = { devId, name, type, value, unit, notes ->
                    viewModel.logManualReading(devId, name, type, value, unit, notes)
                    showLogReadingDialog = false
                }
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
