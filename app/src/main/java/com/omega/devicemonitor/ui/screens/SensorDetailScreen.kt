package com.omega.devicemonitor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import com.omega.devicemonitor.ui.components.ConfirmActionDialog
import com.omega.devicemonitor.ui.components.LogReadingDialog
import com.omega.devicemonitor.ui.components.SensorBadge
import com.omega.devicemonitor.ui.components.SensorTypeHelper
import com.omega.devicemonitor.ui.components.TelemetrySparklineChart
import com.omega.devicemonitor.ui.viewmodel.DeviceMonitorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(
    deviceId: Long,
    sensorName: String,
    sensorType: String,
    viewModel: DeviceMonitorViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val readings by viewModel.readings.collectAsStateWithLifecycle()
    val isFahrenheit by viewModel.tempUnitFahrenheit.collectAsStateWithLifecycle()

    var selectedInterval by remember { mutableStateOf("24h") } // "1h", "24h", "7d", "30d", "ALL"
    var showLogDialog by remember { mutableStateOf(false) }
    var readingToDelete by remember { mutableStateOf<SensorReadingEntity?>(null) }

    val currentDevice = remember(devices, deviceId) {
        devices.firstOrNull { it.id == deviceId }
    }

    // Filter readings for this device & sensor name
    val sensorReadings = remember(readings, deviceId, sensorName) {
        readings.filter { it.deviceId == deviceId && it.sensorName.equals(sensorName, ignoreCase = true) }
    }

    // Filter by selected interval
    val filteredReadings = remember(sensorReadings, selectedInterval) {
        val now = System.currentTimeMillis()
        val cutoff = when (selectedInterval) {
            "1h" -> now - 3_600_000L
            "24h" -> now - 86_400_000L
            "7d" -> now - 7 * 86_400_000L
            "30d" -> now - 30 * 86_400_000L
            else -> 0L
        }
        sensorReadings.filter { it.timestamp >= cutoff }
    }

    val latestReading = sensorReadings.firstOrNull()
    val unit = latestReading?.unit ?: SensorTypeHelper.getDefaultUnit(sensorType)
    val sensorColor = SensorTypeHelper.getColor(sensorType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sensorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentDevice?.name ?: "Device"} • ${sensorType.replace('_', ' ')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("sensor_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    SensorBadge(
                        sensorType = sensorType,
                        size = 36.dp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                modifier = Modifier.testTag("sensor_detail_log_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Log Reading")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Time Window Filter Selector
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("1h" to "1 Hour", "24h" to "24 Hours", "7d" to "7 Days", "30d" to "30 Days", "ALL" to "All")) { (key, label) ->
                        FilterChip(
                            selected = selectedInterval == key,
                            onClick = { selectedInterval = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("interval_filter_$key")
                        )
                    }
                }
            }

            // Interactive Telemetry Chart
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sensor History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TelemetrySparklineChart(
                            readings = filteredReadings,
                            lineColor = sensorColor,
                            unit = unit,
                            isFahrenheit = isFahrenheit,
                            sensorType = sensorType
                        )
                    }
                }
            }

            // Device & Hardware Association Information
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sensor Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Associated Node", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = currentDevice?.name ?: "Unknown", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Node IP / Endpoint", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = currentDevice?.ipAddress ?: "Local Only", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Sensor Category", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = sensorType, fontWeight = FontWeight.SemiBold, color = sensorColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Active Readings Count", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${sensorReadings.size} total entries", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Readings History Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Readings (${filteredReadings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (filteredReadings.isEmpty()) {
                item {
                    Text(
                        text = "No readings recorded during this timeframe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            // Historical items
            items(filteredReadings) { r ->
                val (formattedVal, displayUnit) = SensorTypeHelper.formatValue(
                    r.value,
                    r.unit,
                    isFahrenheit,
                    r.sensorType
                )
                val sdf = remember { SimpleDateFormat("MMM d, yyyy  HH:mm:ss", Locale.getDefault()) }
                val timeFormatted = remember(r.timestamp) { sdf.format(Date(r.timestamp)) }

                ElevatedCard(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$formattedVal $displayUnit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = sensorColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (r.isManual) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (r.isManual) "MANUAL" else "HARDWARE",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (r.isManual) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (r.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "“${r.notes}”",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(
                            onClick = { readingToDelete = r },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Reading",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog && currentDevice != null) {
        LogReadingDialog(
            devices = listOf(currentDevice),
            preselectedDeviceId = currentDevice.id,
            preselectedSensorName = sensorName,
            preselectedSensorType = sensorType,
            onDismiss = { showLogDialog = false },
            onConfirm = { devId, name, type, value, u, notes ->
                viewModel.logManualReading(devId, name, type, value, u, notes)
                showLogDialog = false
            }
        )
    }

    readingToDelete?.let { r ->
        ConfirmActionDialog(
            title = "Delete Reading Entry?",
            message = "Remove reading value '${r.value} ${r.unit}' recorded at ${SensorTypeHelper.formatRelativeTime(r.timestamp)}?",
            confirmButtonText = "Delete",
            isDestructive = true,
            onDismiss = { readingToDelete = null },
            onConfirm = {
                viewModel.deleteReading(r)
                readingToDelete = null
            }
        )
    }
}
