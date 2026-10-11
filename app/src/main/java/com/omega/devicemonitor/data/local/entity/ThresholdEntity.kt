package com.omega.devicemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Configurable numeric threshold rule for triggering alerts.
 * Scoped to user for personal monitoring boundaries.
 */
@Entity(
    tableName = "thresholds",
    indices = [Index(value = ["userId"])]
)
data class ThresholdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val deviceId: Long? = null, // null means applies to all devices matching sensor
    val sensorName: String,
    val sensorType: String,
    val condition: String, // "ABOVE", "BELOW", "OUT_OF_RANGE"
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val severity: String = "WARNING", // "INFO", "WARNING", "CRITICAL"
    val enabled: Boolean = true,
    val customMessage: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
