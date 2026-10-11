package com.omega.devicemonitor.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.omega.devicemonitor.data.local.entity.AlertLogEntity
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.local.entity.ThresholdEntity
import com.omega.devicemonitor.ui.components.AddThresholdDialog
import com.omega.devicemonitor.ui.components.ConfirmActionDialog
import com.omega.devicemonitor.ui.components.SensorBadge
import com.omega.devicemonitor.ui.components.SensorTypeHelper
import com.omega.devicemonitor.ui.theme.TechAmber
import com.omega.devicemonitor.ui.theme.TechCoral
import com.omega.devicemonitor.ui.viewmodel.DeviceMonitorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: DeviceMonitorViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: History, 1: Threshold Rules

    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val unackCount by viewModel.unacknowledgedAlertCount.collectAsStateWithLifecycle()
    val thresholds by viewModel.thresholds.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    var showAddThresholdDialog by remember { mutableStateOf(false) }
    var thresholdToDelete by remember { mutableStateOf<ThresholdEntity?>(null) }
    var showClearAlertsConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Notifications and safety thresholds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Alert Log")
                            if (unackCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Text(
                                        text = "$unackCount",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_alert_log")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Threshold Rules (${thresholds.size})") },
                    modifier = Modifier.testTag("tab_threshold_rules")
                )
            }

            if (selectedTab == 0) {
                // Tab 0: Alert Log
                AlertLogTabContent(
                    alerts = alerts,
                    unackCount = unackCount,
                    onAcknowledge = { viewModel.acknowledgeAlert(it) },
                    onAcknowledgeAll = { viewModel.acknowledgeAllAlerts() },
                    onDelete = { viewModel.deleteAlert(it) },
                    onClearAll = { showClearAlertsConfirm = true }
                )
            } else {
                // Tab 1: Threshold Rules
                ThresholdRulesTabContent(
                    thresholds = thresholds,
                    devices = devices,
                    onToggle = { viewModel.toggleThreshold(it) },
                    onDelete = { thresholdToDelete = it },
                    onAdd = { showAddThresholdDialog = true }
                )
            }
        }

        if (selectedTab == 1) {
            FloatingActionButton(
                onClick = { showAddThresholdDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_threshold_fab"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Threshold")
            }
        }
    }

    if (showAddThresholdDialog) {
        AddThresholdDialog(
            devices = devices,
            onDismiss = { showAddThresholdDialog = false },
            onConfirm = { devId, name, type, cond, min, max, sev, msg ->
                viewModel.addThreshold(devId, name, type, cond, min, max, sev, msg)
                showAddThresholdDialog = false
            }
        )
    }

    thresholdToDelete?.let { t ->
        ConfirmActionDialog(
            title = "Delete Threshold Rule?",
            message = "Remove threshold rule for ${t.sensorType} (${t.condition})? Triggered alerts history will be retained.",
            confirmButtonText = "Delete Rule",
            isDestructive = true,
            onDismiss = { thresholdToDelete = null },
            onConfirm = {
                viewModel.deleteThreshold(t)
                thresholdToDelete = null
            }
        )
    }

    if (showClearAlertsConfirm) {
        ConfirmActionDialog(
            title = "Clear Alert History?",
            message = "This will permanently delete all past alert history records. Active threshold rules will not be affected.",
            confirmButtonText = "Clear History",
            isDestructive = true,
            onDismiss = { showClearAlertsConfirm = false },
            onConfirm = {
                viewModel.clearAlertHistory()
                showClearAlertsConfirm = false
            }
        )
    }
}

@Composable
private fun AlertLogTabContent(
    alerts: List<AlertLogEntity>,
    unackCount: Int,
    onAcknowledge: (Long) -> Unit,
    onAcknowledgeAll: () -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (alerts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (unackCount > 0) {
                        OutlinedButton(
                            onClick = onAcknowledgeAll,
                            modifier = Modifier.testTag("ack_all_alerts_button")
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Acknowledge All ($unackCount)")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    OutlinedButton(
                        onClick = onClearAll,
                        modifier = Modifier.testTag("clear_all_alerts_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }

        if (alerts.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Alerts Triggered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All telemetry streams are within safe operational thresholds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(alerts) { alert ->
            val sevColor = when (alert.severity.uppercase()) {
                "CRITICAL" -> TechCoral
                "WARNING" -> TechAmber
                else -> MaterialTheme.colorScheme.primary
            }

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (!alert.acknowledged)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .testTag("alert_item_${alert.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(sevColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = sevColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = sevColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = alert.severity,
                                    color = sevColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alert.sensorName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${alert.deviceName} • ${SensorTypeHelper.formatRelativeTime(alert.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (!alert.acknowledged) {
                            Button(
                                onClick = { onAcknowledge(alert.id) },
                                modifier = Modifier.testTag("ack_button_${alert.id}")
                            ) {
                                Text("Acknowledge", fontSize = 11.sp)
                            }
                        } else {
                            IconButton(onClick = { onDelete(alert.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThresholdRulesTabContent(
    thresholds: List<ThresholdEntity>,
    devices: List<DeviceEntity>,
    onToggle: (ThresholdEntity) -> Unit,
    onDelete: (ThresholdEntity) -> Unit,
    onAdd: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (thresholds.isEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Rule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Threshold Rules Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Define minimum/maximum numeric thresholds to automatically receive alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onAdd) {
                            Text("Create Threshold Rule")
                        }
                    }
                }
            }
        }

        items(thresholds) { rule ->
            val devName = if (rule.deviceId != null) {
                devices.firstOrNull { it.id == rule.deviceId }?.name ?: "Device #${rule.deviceId}"
            } else {
                "All Connected Devices"
            }

            val conditionDesc = when (rule.condition) {
                "ABOVE" -> "> ${rule.maxValue ?: rule.minValue}"
                "BELOW" -> "< ${rule.minValue ?: rule.maxValue}"
                "OUT_OF_RANGE" -> "[${rule.minValue} .. ${rule.maxValue}]"
                else -> rule.condition
            }

            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .testTag("threshold_card_${rule.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorBadge(sensorType = rule.sensorType, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rule.sensorType.replace('_', ' ')} $conditionDesc",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Applies to: $devName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (rule.customMessage.isNotBlank()) {
                            Text(
                                text = "“${rule.customMessage}”",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { onToggle(rule) },
                            modifier = Modifier.testTag("threshold_switch_${rule.id}")
                        )
                        IconButton(onClick = { onDelete(rule) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Rule",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
