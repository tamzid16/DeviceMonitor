package com.omega.devicemonitor.data.repository

import com.omega.devicemonitor.data.local.DeviceMonitorDatabase
import com.omega.devicemonitor.data.local.entity.AlertLogEntity
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import com.omega.devicemonitor.data.local.entity.ThresholdEntity
import com.omega.devicemonitor.data.local.entity.UserProfileEntity
import com.omega.devicemonitor.util.SecurityHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class DeviceMonitorRepository(private val db: DeviceMonitorDatabase) {

    private val deviceDao = db.deviceDao()
    private val sensorReadingDao = db.sensorReadingDao()
    private val thresholdDao = db.thresholdDao()
    private val alertLogDao = db.alertLogDao()
    private val userDao = db.userDao()

    // User Accounts & Authentication
    val allUsers: Flow<List<UserProfileEntity>> = userDao.getAllUsers()

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        avatarIndex: Int = 0
    ): Result<UserProfileEntity> {
        val cleanEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists."))
        }
        val hash = SecurityHelper.hashPassword(password)
        val user = UserProfileEntity(
            name = name.trim(),
            email = cleanEmail,
            passwordHash = hash,
            avatarColorIndex = avatarIndex,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        val id = userDao.insertUser(user)
        val created = userDao.getUserById(id)
        if (created != null) {
            return Result.success(created)
        }
        return Result.failure(Exception("Failed to register account."))
    }

    suspend fun loginUser(email: String, password: String): Result<UserProfileEntity> {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(cleanEmail)
            ?: return Result.failure(Exception("No account found with this email."))

        if (!SecurityHelper.verifyPassword(password, user.passwordHash)) {
            return Result.failure(Exception("Incorrect password. Please try again."))
        }

        val updated = user.copy(
            passwordHash = if (SecurityHelper.needsRehash(user.passwordHash)) {
                SecurityHelper.hashPassword(password)
            } else {
                user.passwordHash
            },
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        return Result.success(updated)
    }

    suspend fun getUserById(id: Long): UserProfileEntity? = userDao.getUserById(id)

    suspend fun getUserByEmail(email: String): UserProfileEntity? = userDao.getUserByEmail(email)

    suspend fun updateUserProfile(user: UserProfileEntity) = userDao.updateUser(user)

    suspend fun deleteUserAccount(userId: Long) {
        // Cascade delete isolated user data
        alertLogDao.deleteAlertsForUser(userId)
        thresholdDao.deleteThresholdsForUser(userId)
        sensorReadingDao.deleteReadingsForUser(userId)
        deviceDao.deleteDevicesForUser(userId)
        userDao.deleteUserById(userId)
    }

    // Per-User Reactive Streams
    fun getDevicesForUser(userId: Long): Flow<List<DeviceEntity>> = deviceDao.getDevicesForUser(userId)
    fun getReadingsForUser(userId: Long): Flow<List<SensorReadingEntity>> = sensorReadingDao.getReadingsForUser(userId)
    fun getRecentReadingsForUser(userId: Long): Flow<List<SensorReadingEntity>> = sensorReadingDao.getRecentReadingsForUser(userId, 60)
    fun getThresholdsForUser(userId: Long): Flow<List<ThresholdEntity>> = thresholdDao.getThresholdsForUser(userId)
    fun getAlertsForUser(userId: Long): Flow<List<AlertLogEntity>> = alertLogDao.getAlertsForUser(userId)
    fun getUnacknowledgedAlertsForUser(userId: Long): Flow<List<AlertLogEntity>> = alertLogDao.getUnacknowledgedAlertsForUser(userId)
    fun getUnacknowledgedCountForUser(userId: Long): Flow<Int> = alertLogDao.getUnacknowledgedCountForUser(userId)
    fun getTotalAlertCountForUser(userId: Long): Flow<Int> = alertLogDao.getTotalAlertCountForUser(userId)
    fun getDeviceCountForUser(userId: Long): Flow<Int> = deviceDao.getDeviceCountForUser(userId)
    fun getReadingCountForUser(userId: Long): Flow<Int> = sensorReadingDao.getReadingCountForUser(userId)
    fun getThresholdCountForUser(userId: Long): Flow<Int> = thresholdDao.getThresholdCountForUser(userId)

    // Devices (Scoped to user)
    suspend fun insertDevice(device: DeviceEntity): Long = deviceDao.insertDevice(device)
    suspend fun updateDevice(device: DeviceEntity) = deviceDao.updateDevice(device)
    suspend fun deleteDevice(device: DeviceEntity) = deviceDao.deleteDevice(device)
    suspend fun deleteDeviceById(id: Long) = deviceDao.deleteDeviceById(id)
    suspend fun getDeviceById(id: Long): DeviceEntity? = deviceDao.getDeviceById(id)

    // Readings & Alert Evaluation Engine (Scoped to user)
    suspend fun insertReading(reading: SensorReadingEntity): Long {
        val id = sensorReadingDao.insertReading(reading)
        val device = deviceDao.getDeviceById(reading.deviceId)
        if (device != null) {
            deviceDao.updateDevice(
                device.copy(
                    lastSeenTimestamp = reading.timestamp,
                    status = "ONLINE"
                )
            )
        }
        evaluateThresholdsForReading(reading, device?.name ?: "Unknown Node")
        return id
    }

    suspend fun deleteReading(reading: SensorReadingEntity) = sensorReadingDao.deleteReading(reading)
    suspend fun deleteReadingById(id: Long) = sensorReadingDao.deleteReadingById(id)

    fun getReadingsForSensor(deviceId: Long, sensorName: String): Flow<List<SensorReadingEntity>> =
        sensorReadingDao.getReadingsForSensor(deviceId, sensorName)

    fun getReadingsForSensorSince(deviceId: Long, sensorName: String, sinceTimestamp: Long): Flow<List<SensorReadingEntity>> =
        sensorReadingDao.getReadingsForSensorSince(deviceId, sensorName, sinceTimestamp)

    fun getReadingsForDevice(deviceId: Long): Flow<List<SensorReadingEntity>> =
        sensorReadingDao.getReadingsForDevice(deviceId)

    private suspend fun evaluateThresholdsForReading(reading: SensorReadingEntity, deviceName: String) {
        val activeThresholds = thresholdDao.getActiveThresholdsForUser(reading.userId)
        for (threshold in activeThresholds) {
            if (threshold.deviceId != null && threshold.deviceId != reading.deviceId) continue

            val matchesType = threshold.sensorType.equals(reading.sensorType, ignoreCase = true)
            val matchesName = threshold.sensorName.isEmpty() ||
                    threshold.sensorName.equals(reading.sensorName, ignoreCase = true)

            if (!matchesType && !matchesName) continue

            var breached = false
            var message = threshold.customMessage

            when (threshold.condition) {
                "ABOVE" -> {
                    val max = threshold.maxValue ?: threshold.minValue ?: 0.0
                    if (reading.value > max) {
                        breached = true
                        if (message.isBlank()) {
                            message = "${reading.sensorName} exceeded limit (${reading.value} ${reading.unit} > $max ${reading.unit})"
                        }
                    }
                }
                "BELOW" -> {
                    val min = threshold.minValue ?: threshold.maxValue ?: 0.0
                    if (reading.value < min) {
                        breached = true
                        if (message.isBlank()) {
                            message = "${reading.sensorName} dropped below limit (${reading.value} ${reading.unit} < $min ${reading.unit})"
                        }
                    }
                }
                "OUT_OF_RANGE" -> {
                    val min = threshold.minValue ?: 0.0
                    val max = threshold.maxValue ?: 100.0
                    if (reading.value < min || reading.value > max) {
                        breached = true
                        if (message.isBlank()) {
                            message = "${reading.sensorName} out of safe range [${min}..${max}] (${reading.value} ${reading.unit})"
                        }
                    }
                }
            }

            if (breached) {
                alertLogDao.insertAlert(
                    AlertLogEntity(
                        thresholdId = threshold.id,
                        userId = reading.userId,
                        deviceId = reading.deviceId,
                        deviceName = deviceName,
                        sensorName = reading.sensorName,
                        sensorType = reading.sensorType,
                        triggeredValue = reading.value,
                        unit = reading.unit,
                        message = message,
                        severity = threshold.severity,
                        timestamp = reading.timestamp,
                        acknowledged = false
                    )
                )
            }
        }
    }

    // Thresholds
    suspend fun insertThreshold(threshold: ThresholdEntity): Long = thresholdDao.insertThreshold(threshold)
    suspend fun updateThreshold(threshold: ThresholdEntity) = thresholdDao.updateThreshold(threshold)
    suspend fun deleteThreshold(threshold: ThresholdEntity) = thresholdDao.deleteThreshold(threshold)

    // Alerts
    suspend fun acknowledgeAlert(id: Long) = alertLogDao.acknowledgeAlert(id)
    suspend fun acknowledgeAllAlertsForUser(userId: Long) = alertLogDao.acknowledgeAllAlertsForUser(userId)
    suspend fun deleteAlertById(id: Long) = alertLogDao.deleteAlertById(id)
    suspend fun clearAlertsForUser(userId: Long) = alertLogDao.deleteAlertsForUser(userId)

    // Data Management
    suspend fun clearSensorHistoryForUser(userId: Long) {
        sensorReadingDao.deleteReadingsForUser(userId)
    }

    suspend fun resetUserData(userId: Long) {
        alertLogDao.deleteAlertsForUser(userId)
        thresholdDao.deleteThresholdsForUser(userId)
        sensorReadingDao.deleteReadingsForUser(userId)
        deviceDao.deleteDevicesForUser(userId)
    }

    suspend fun exportUserDataAsJson(userId: Long): String {
        val root = JSONObject()
        root.put("appName", "DeviceMonitor")
        root.put("version", "2.0")
        root.put("exportedAt", System.currentTimeMillis())

        val user = userDao.getUserById(userId)
        if (user != null) {
            val userObj = JSONObject()
            userObj.put("name", user.name)
            userObj.put("email", user.email)
            userObj.put("avatarColorIndex", user.avatarColorIndex)
            userObj.put("tempUnitFahrenheit", user.tempUnitFahrenheit)
            root.put("userProfile", userObj)
        }

        // Devices
        val devices = deviceDao.getDevicesForUserDirect(userId)
        val devArray = JSONArray()
        val idToDevIndexMap = mutableMapOf<Long, Int>()
        devices.forEachIndexed { index, dev ->
            idToDevIndexMap[dev.id] = index
            val d = JSONObject()
            d.put("index", index)
            d.put("name", dev.name)
            d.put("location", dev.location)
            d.put("ipAddress", dev.ipAddress)
            d.put("deviceType", dev.deviceType)
            d.put("status", dev.status)
            d.put("notes", dev.notes)
            d.put("createdAt", dev.createdAt)
            devArray.put(d)
        }
        root.put("devices", devArray)

        // Thresholds
        val thresholds = thresholdDao.getThresholdsForUserDirect(userId)
        val threshArray = JSONArray()
        for (th in thresholds) {
            val tObj = JSONObject()
            tObj.put("sensorName", th.sensorName)
            tObj.put("sensorType", th.sensorType)
            tObj.put("condition", th.condition)
            if (th.minValue != null) tObj.put("minValue", th.minValue)
            if (th.maxValue != null) tObj.put("maxValue", th.maxValue)
            tObj.put("severity", th.severity)
            tObj.put("enabled", th.enabled)
            tObj.put("customMessage", th.customMessage)
            threshArray.put(tObj)
        }
        root.put("thresholds", threshArray)

        // Sensor Readings
        val readings = sensorReadingDao.getReadingsForUserDirect(userId)
        val readArray = JSONArray()
        for (r in readings) {
            val rObj = JSONObject()
            val devIdx = idToDevIndexMap[r.deviceId] ?: 0
            rObj.put("deviceIndex", devIdx)
            rObj.put("sensorName", r.sensorName)
            rObj.put("sensorType", r.sensorType)
            rObj.put("value", r.value)
            rObj.put("unit", r.unit)
            rObj.put("timestamp", r.timestamp)
            rObj.put("isManual", r.isManual)
            rObj.put("notes", r.notes)
            readArray.put(rObj)
        }
        root.put("readings", readArray)

        // Alerts
        val alerts = alertLogDao.getAlertsForUserDirect(userId)
        val alertArray = JSONArray()
        for (a in alerts) {
            val aObj = JSONObject()
            val devIdx = idToDevIndexMap[a.deviceId] ?: 0
            aObj.put("deviceIndex", devIdx)
            aObj.put("deviceName", a.deviceName)
            aObj.put("sensorName", a.sensorName)
            aObj.put("sensorType", a.sensorType)
            aObj.put("triggeredValue", a.triggeredValue)
            aObj.put("unit", a.unit)
            aObj.put("message", a.message)
            aObj.put("severity", a.severity)
            aObj.put("timestamp", a.timestamp)
            aObj.put("acknowledged", a.acknowledged)
            alertArray.put(aObj)
        }
        root.put("alerts", alertArray)

        return root.toString(2)
    }

    suspend fun importUserDataFromJson(userId: Long, jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("devices") && !root.has("userProfile") && !root.has("readings")) {
                return Result.failure(Exception("Invalid DeviceMonitor backup format"))
            }

            // Sync user preferences if present
            if (root.has("userProfile")) {
                val userObj = root.getJSONObject("userProfile")
                val currentUser = userDao.getUserById(userId)
                if (currentUser != null) {
                    val updated = currentUser.copy(
                        tempUnitFahrenheit = userObj.optBoolean("tempUnitFahrenheit", currentUser.tempUnitFahrenheit),
                        avatarColorIndex = userObj.optInt("avatarColorIndex", currentUser.avatarColorIndex)
                    )
                    userDao.updateUser(updated)
                }
            }

            // Import devices
            val devIndexToNewId = mutableMapOf<Int, Long>()
            var devCount = 0
            if (root.has("devices")) {
                val devArray = root.getJSONArray("devices")
                for (i in 0 until devArray.length()) {
                    val d = devArray.getJSONObject(i)
                    val idx = d.optInt("index", i)
                    val newId = deviceDao.insertDevice(
                        DeviceEntity(
                            userId = userId,
                            name = d.optString("name", "Imported Device"),
                            location = d.optString("location", "Home"),
                            ipAddress = d.optString("ipAddress", ""),
                            deviceType = d.optString("deviceType", "ESP32"),
                            status = d.optString("status", "ONLINE"),
                            notes = d.optString("notes", ""),
                            createdAt = d.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                    devIndexToNewId[idx] = newId
                    devCount++
                }
            }

            // Fallback device ID if none exist
            val fallbackDevId = devIndexToNewId.values.firstOrNull() ?: 0L

            // Import Thresholds
            var threshCount = 0
            if (root.has("thresholds")) {
                val threshArray = root.getJSONArray("thresholds")
                for (i in 0 until threshArray.length()) {
                    val t = threshArray.getJSONObject(i)
                    thresholdDao.insertThreshold(
                        ThresholdEntity(
                            userId = userId,
                            deviceId = null,
                            sensorName = t.optString("sensorName", ""),
                            sensorType = t.optString("sensorType", "CUSTOM"),
                            condition = t.optString("condition", "ABOVE"),
                            minValue = if (t.has("minValue")) t.optDouble("minValue") else null,
                            maxValue = if (t.has("maxValue")) t.optDouble("maxValue") else null,
                            severity = t.optString("severity", "WARNING"),
                            enabled = t.optBoolean("enabled", true),
                            customMessage = t.optString("customMessage", "")
                        )
                    )
                    threshCount++
                }
            }

            // Import Readings
            var readCount = 0
            if (root.has("readings")) {
                val readArray = root.getJSONArray("readings")
                val readingsToInsert = mutableListOf<SensorReadingEntity>()
                for (i in 0 until readArray.length()) {
                    val r = readArray.getJSONObject(i)
                    val devIdx = r.optInt("deviceIndex", 0)
                    val targetDevId = devIndexToNewId[devIdx] ?: fallbackDevId
                    if (targetDevId > 0L) {
                        readingsToInsert.add(
                            SensorReadingEntity(
                                userId = userId,
                                deviceId = targetDevId,
                                sensorName = r.optString("sensorName", "Sensor"),
                                sensorType = r.optString("sensorType", "CUSTOM"),
                                value = r.optDouble("value", 0.0),
                                unit = r.optString("unit", ""),
                                timestamp = r.optLong("timestamp", System.currentTimeMillis()),
                                isManual = r.optBoolean("isManual", false),
                                notes = r.optString("notes", "")
                            )
                        )
                        readCount++
                    }
                }
                if (readingsToInsert.isNotEmpty()) {
                    sensorReadingDao.insertReadings(readingsToInsert)
                }
            }

            // Import Alerts
            var alertCount = 0
            if (root.has("alerts")) {
                val alertArray = root.getJSONArray("alerts")
                val alertsToInsert = mutableListOf<AlertLogEntity>()
                for (i in 0 until alertArray.length()) {
                    val a = alertArray.getJSONObject(i)
                    val devIdx = a.optInt("deviceIndex", 0)
                    val targetDevId = devIndexToNewId[devIdx] ?: fallbackDevId
                    alertsToInsert.add(
                        AlertLogEntity(
                            userId = userId,
                            deviceId = targetDevId,
                            deviceName = a.optString("deviceName", "Device"),
                            sensorName = a.optString("sensorName", "Sensor"),
                            sensorType = a.optString("sensorType", "CUSTOM"),
                            triggeredValue = a.optDouble("triggeredValue", 0.0),
                            unit = a.optString("unit", ""),
                            message = a.optString("message", "Alert triggered"),
                            severity = a.optString("severity", "WARNING"),
                            timestamp = a.optLong("timestamp", System.currentTimeMillis()),
                            acknowledged = a.optBoolean("acknowledged", false)
                        )
                    )
                    alertCount++
                }
                if (alertsToInsert.isNotEmpty()) {
                    alertLogDao.insertAlerts(alertsToInsert)
                }
            }

            Result.success("Imported $devCount devices, $readCount readings, $threshCount rules, $alertCount alerts.")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to restore backup: ${e.localizedMessage}"))
        }
    }

    suspend fun getUserStorageStats(userId: Long): StorageStats {
        return StorageStats(
            devices = deviceDao.getDeviceCountForUserDirect(userId),
            readings = sensorReadingDao.getReadingCountForUserDirect(userId),
            thresholds = thresholdDao.getThresholdCountForUserDirect(userId),
            alerts = alertLogDao.getTotalAlertCountForUserDirect(userId),
            users = userDao.getUserCountDirect()
        )
    }

    suspend fun seedStarterDataForUser(userId: Long) {
        val now = System.currentTimeMillis()

        // 1. Devices
        val dev1Id = deviceDao.insertDevice(
            DeviceEntity(
                userId = userId,
                name = "ESP32 Living Room Hub",
                location = "Living Room",
                ipAddress = "192.168.1.120",
                deviceType = "ESP32",
                status = "ONLINE",
                lastSeenTimestamp = now,
                notes = "ESP32-WROOM-32 with DHT22, BH1750 and 18650 battery backup"
            )
        )

        val dev2Id = deviceDao.insertDevice(
            DeviceEntity(
                userId = userId,
                name = "Balcony Weather Station",
                location = "Balcony",
                ipAddress = "192.168.1.145",
                deviceType = "ESP32",
                status = "ONLINE",
                lastSeenTimestamp = now - 120_000,
                notes = "Solar-powered ESP32 with BMP280 pressure and capacitive soil sensor"
            )
        )

        val dev3Id = deviceDao.insertDevice(
            DeviceEntity(
                userId = userId,
                name = "Hydroponic Garden Node",
                location = "Indoor Greenery",
                ipAddress = "192.168.1.168",
                deviceType = "Raspberry Pi Pico",
                status = "IDLE",
                lastSeenTimestamp = now - 360_000,
                notes = "Pico W monitoring water flow and reservoir level"
            )
        )

        // 2. Thresholds
        thresholdDao.insertThreshold(
            ThresholdEntity(
                userId = userId,
                deviceId = null,
                sensorName = "Soil Moisture",
                sensorType = "SOIL_MOISTURE",
                condition = "BELOW",
                minValue = 30.0,
                severity = "CRITICAL",
                customMessage = "Soil moisture critically dry! Water plants immediately."
            )
        )
        thresholdDao.insertThreshold(
            ThresholdEntity(
                userId = userId,
                deviceId = null,
                sensorName = "Ambient Temp",
                sensorType = "TEMPERATURE",
                condition = "ABOVE",
                maxValue = 32.0,
                severity = "WARNING",
                customMessage = "High ambient temperature detected in room."
            )
        )
        thresholdDao.insertThreshold(
            ThresholdEntity(
                userId = userId,
                deviceId = null,
                sensorName = "Battery Level",
                sensorType = "BATTERY",
                condition = "BELOW",
                minValue = 20.0,
                severity = "WARNING",
                customMessage = "Node backup battery is running low."
            )
        )

        // 3. Historical Telemetry (Past 12 hours)
        val sampleReadings = mutableListOf<SensorReadingEntity>()
        val oneHour = 3_600_000L

        for (i in 12 downTo 0) {
            val t = now - (i * oneHour)
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev1Id,
                    sensorName = "Ambient Temp",
                    sensorType = "TEMPERATURE",
                    value = 22.4 + (i % 4) * 0.8 + ((i * 3) % 2) * 0.3,
                    unit = "°C",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev1Id,
                    sensorName = "Air Humidity",
                    sensorType = "HUMIDITY",
                    value = 48.0 + (i % 5) * 1.5,
                    unit = "%",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev1Id,
                    sensorName = "Ambient Light",
                    sensorType = "LIGHT",
                    value = if (i in 2..8) 450.0 + (i * 25) else 35.0,
                    unit = "lux",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev1Id,
                    sensorName = "Hub Battery",
                    sensorType = "BATTERY",
                    value = 98.0 - (i * 0.4),
                    unit = "%",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev2Id,
                    sensorName = "Soil Moisture",
                    sensorType = "SOIL_MOISTURE",
                    value = 38.0 - (i * 0.8),
                    unit = "%",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev2Id,
                    sensorName = "Barometric Pressure",
                    sensorType = "PRESSURE",
                    value = 1012.5 + ((i % 3) * 0.7),
                    unit = "hPa",
                    timestamp = t,
                    isManual = false
                )
            )
            sampleReadings.add(
                SensorReadingEntity(
                    userId = userId,
                    deviceId = dev3Id,
                    sensorName = "Water Flow Rate",
                    sensorType = "CUSTOM",
                    value = 3.2 + (i % 2) * 0.4,
                    unit = "L/min",
                    timestamp = t,
                    isManual = false
                )
            )
        }

        // Add 2 manual entries
        sampleReadings.add(
            SensorReadingEntity(
                userId = userId,
                deviceId = dev1Id,
                sensorName = "Manual Hygrometer Check",
                sensorType = "HUMIDITY",
                value = 52.0,
                unit = "%",
                timestamp = now - 1800_000L,
                isManual = true,
                notes = "Analog hygrometer cross-check"
            )
        )
        sampleReadings.add(
            SensorReadingEntity(
                userId = userId,
                deviceId = dev2Id,
                sensorName = "Soil Moisture",
                sensorType = "SOIL_MOISTURE",
                value = 26.5,
                unit = "%",
                timestamp = now - 900_000L,
                isManual = true,
                notes = "Manual test probe in corner pot"
            )
        )

        sensorReadingDao.insertReadings(sampleReadings)

        // Seed 1 active alert
        alertLogDao.insertAlert(
            AlertLogEntity(
                thresholdId = 1L,
                userId = userId,
                deviceId = dev2Id,
                deviceName = "Balcony Weather Station",
                sensorName = "Soil Moisture",
                sensorType = "SOIL_MOISTURE",
                triggeredValue = 26.5,
                unit = "%",
                message = "Soil moisture critically dry! Water plants immediately.",
                severity = "CRITICAL",
                timestamp = now - 900_000L,
                acknowledged = false
            )
        )
    }

}

data class StorageStats(
    val devices: Int,
    val readings: Int,
    val thresholds: Int,
    val alerts: Int,
    val users: Int = 0
)
