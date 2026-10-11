package com.omega.devicemonitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omega.devicemonitor.ui.theme.SensorColorBattery
import com.omega.devicemonitor.ui.theme.SensorColorCustom
import com.omega.devicemonitor.ui.theme.SensorColorHumidity
import com.omega.devicemonitor.ui.theme.SensorColorLight
import com.omega.devicemonitor.ui.theme.SensorColorPressure
import com.omega.devicemonitor.ui.theme.SensorColorSoil
import com.omega.devicemonitor.ui.theme.SensorColorTemp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SensorTypeHelper {
    val ALL_TYPES = listOf(
        "TEMPERATURE" to "Temperature",
        "HUMIDITY" to "Humidity",
        "SOIL_MOISTURE" to "Soil Moisture",
        "LIGHT" to "Light",
        "PRESSURE" to "Pressure",
        "BATTERY" to "Battery",
        "CUSTOM" to "Custom Numeric"
    )

    fun getColor(type: String): Color {
        return when (type.uppercase()) {
            "TEMPERATURE" -> SensorColorTemp
            "HUMIDITY" -> SensorColorHumidity
            "SOIL_MOISTURE" -> SensorColorSoil
            "LIGHT" -> SensorColorLight
            "PRESSURE" -> SensorColorPressure
            "BATTERY" -> SensorColorBattery
            else -> SensorColorCustom
        }
    }

    fun getIcon(type: String): ImageVector {
        return when (type.uppercase()) {
            "TEMPERATURE" -> Icons.Default.Thermostat
            "HUMIDITY" -> Icons.Default.WaterDrop
            "SOIL_MOISTURE" -> Icons.Default.Grass
            "LIGHT" -> Icons.Default.LightMode
            "PRESSURE" -> Icons.Default.Compress
            "BATTERY" -> Icons.Default.BatteryChargingFull
            else -> Icons.Default.Tune
        }
    }

    fun getDefaultUnit(type: String): String {
        return when (type.uppercase()) {
            "TEMPERATURE" -> "°C"
            "HUMIDITY", "SOIL_MOISTURE", "BATTERY" -> "%"
            "LIGHT" -> "lux"
            "PRESSURE" -> "hPa"
            else -> "units"
        }
    }

    fun formatValue(value: Double, unit: String, isFahrenheit: Boolean = false, sensorType: String = ""): Pair<String, String> {
        return if (sensorType.equals("TEMPERATURE", ignoreCase = true) && isFahrenheit && unit.contains("C")) {
            val f = (value * 9.0 / 5.0) + 32.0
            String.format(Locale.US, "%.1f", f) to "°F"
        } else {
            String.format(Locale.US, "%.1f", value) to unit
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            diff < 30_000 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}

@Composable
fun SensorBadge(
    sensorType: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val color = SensorTypeHelper.getColor(sensorType)
    val icon = SensorTypeHelper.getIcon(sensorType)

    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = 0.16f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = sensorType,
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
