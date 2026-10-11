package com.omega.devicemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Log of triggered alert events.
 * Scoped to user for personalized alerts history.
 */
@Entity(
    tableName = "alert_logs",
    indices = [Index(value = ["userId"])]
)
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val thresholdId: Long? = null,
    val deviceId: Long,
    val deviceName: String,
    val sensorName: String,
    val sensorType: String,
    val triggeredValue: Double,
    val unit: String,
    val message: String,
    val severity: String = "WARNING", // "INFO", "WARNING", "CRITICAL"
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false
)
