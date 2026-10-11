package com.omega.devicemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Flexible sensor telemetry reading entity.
 * All sensor types are treated with equal priority.
 * Associated with user and device for complete data isolation.
 */
@Entity(
    tableName = "sensor_readings",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deviceId"]),
        Index(value = ["userId"]),
        Index(value = ["sensorName"]),
        Index(value = ["sensorType"]),
        Index(value = ["timestamp"])
    ]
)
data class SensorReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: Long,
    val userId: Long,
    val sensorName: String, // e.g. "Air Temp", "Zone 1 Soil", "Lux Ambient", "Battery Level"
    val sensorType: String, // "TEMPERATURE", "HUMIDITY", "SOIL_MOISTURE", "LIGHT", "PRESSURE", "BATTERY", "CUSTOM"
    val value: Double,
    val unit: String, // "°C", "°F", "%", "lux", "hPa", "V", "ppm", "psi", etc.
    val timestamp: Long = System.currentTimeMillis(),
    val isManual: Boolean = false, // Clearly marks whether entered manually or via hardware
    val notes: String = ""
)
