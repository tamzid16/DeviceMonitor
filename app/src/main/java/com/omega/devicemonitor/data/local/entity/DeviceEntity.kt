package com.omega.devicemonitor.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a registered IoT device (ESP32, ESP8266, Pico, Arduino, etc.).
 * Scoped to a specific user for data isolation.
 */
@Entity(
    tableName = "devices",
    indices = [Index(value = ["userId"])]
)
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val location: String,
    val ipAddress: String, // e.g. "192.168.1.105" or hostname
    val deviceType: String, // "ESP32", "ESP8266", "Raspberry Pi Pico", "Arduino", "Generic MCU"
    val status: String = "ONLINE", // "ONLINE", "IDLE", "OFFLINE"
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
