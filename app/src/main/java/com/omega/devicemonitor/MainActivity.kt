package com.omega.devicemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omega.devicemonitor.ui.components.UserProfileChip
import com.omega.devicemonitor.ui.screens.AlertsScreen
import com.omega.devicemonitor.ui.screens.AuthScreen
import com.omega.devicemonitor.ui.screens.DashboardScreen
import com.omega.devicemonitor.ui.screens.DevicesScreen
import com.omega.devicemonitor.ui.screens.SensorDetailScreen
import com.omega.devicemonitor.ui.screens.SettingsScreen
import com.omega.devicemonitor.ui.screens.SplashScreen
import com.omega.devicemonitor.ui.theme.DeviceMonitorTheme
import com.omega.devicemonitor.ui.viewmodel.DeviceMonitorViewModel

enum class MainTab {
    DASHBOARD,
    DEVICES,
    ALERTS,
    SETTINGS
}

data class DetailNav(
    val deviceId: Long,
    val sensorName: String,
    val sensorType: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: DeviceMonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = darkModeOverride ?: systemDark

            DeviceMonitorTheme(darkTheme = useDarkTheme) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

                if (showSplash) {
                    SplashScreen(
                        onFinish = { showSplash = false }
                    )
                } else if (currentUser == null) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    MainAppScaffold(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: DeviceMonitorViewModel
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    var activeDetailNav by remember { mutableStateOf<DetailNav?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val unackAlertCount by viewModel.unacknowledgedAlertCount.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            if (activeDetailNav == null) {
                androidx.compose.foundation.layout.Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "DeviceMonitor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            UserProfileChip(
                                user = currentUser,
                                onClick = { currentTab = MainTab.SETTINGS },
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (activeDetailNav == null) {
                androidx.compose.foundation.layout.Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                    NavigationBar(
                        modifier = Modifier.testTag("main_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.DASHBOARD,
                        onClick = { currentTab = MainTab.DASHBOARD },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.DEVICES,
                        onClick = { currentTab = MainTab.DEVICES },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DeveloperBoard,
                                contentDescription = "Devices"
                            )
                        },
                        label = { Text("Devices") },
                        modifier = Modifier.testTag("nav_item_devices")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.ALERTS,
                        onClick = { currentTab = MainTab.ALERTS },
                        icon = {
                            if (unackAlertCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("$unackAlertCount") }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Alerts"
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alerts"
                                )
                            }
                        },
                        label = { Text("Alerts") },
                        modifier = Modifier.testTag("nav_item_alerts")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { currentTab = MainTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
            val detail = activeDetailNav
            if (detail != null) {
                SensorDetailScreen(
                    deviceId = detail.deviceId,
                    sensorName = detail.sensorName,
                    sensorType = detail.sensorType,
                    viewModel = viewModel,
                    onNavigateBack = { activeDetailNav = null }
                )
            } else {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        MainTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSensorDetail = { devId, sName, sType ->
                                activeDetailNav = DetailNav(devId, sName, sType)
                            },
                            onNavigateToAlerts = { currentTab = MainTab.ALERTS },
                            onNavigateToDevices = { currentTab = MainTab.DEVICES }
                        )

                        MainTab.DEVICES -> DevicesScreen(viewModel = viewModel)

                        MainTab.ALERTS -> AlertsScreen(viewModel = viewModel)

                        MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
            }
        }
    }
}
