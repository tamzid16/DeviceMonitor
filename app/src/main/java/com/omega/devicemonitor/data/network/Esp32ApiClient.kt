package com.omega.devicemonitor.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ParsedReading(
    val sensorName: String,
    val sensorType: String,
    val value: Double,
    val unit: String
)

data class Esp32PollResult(
    val success: Boolean,
    val message: String,
    val readings: List<ParsedReading> = emptyList(),
    val rawJson: String? = null,
    val responseTimeMs: Long = 0
)

/**
 * Clean HTTP client layer for ESP32 and microcontrollers running HTTP servers.
 * Connects to http://<device_ip>/api/sensors or http://<device_ip>/status.
 */
class Esp32ApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    suspend fun pollDevice(ipAddress: String, customPath: String = "/api/sensors"): Esp32PollResult =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var sanitizedIp = ipAddress.trim()
            if (!sanitizedIp.startsWith("http://") && !sanitizedIp.startsWith("https://")) {
                sanitizedIp = "http://$sanitizedIp"
            }
            if (sanitizedIp.endsWith("/")) {
                sanitizedIp = sanitizedIp.substring(0, sanitizedIp.length - 1)
            }
            val targetUrl = "$sanitizedIp${if (customPath.startsWith("/")) customPath else "/$customPath"}"

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "DeviceMonitor-Android/1.0")
                    .header("Accept", "application/json, text/plain")
                    .build()

                val response = client.newCall(request).execute()
                val elapsed = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    return@withContext Esp32PollResult(
                        success = false,
                        message = "HTTP ${response.code}: ${response.message}",
                        responseTimeMs = elapsed
                    )
                }

                val bodyString = response.body?.string() ?: ""
                val readings = parseEsp32Json(bodyString)

                Esp32PollResult(
                    success = true,
                    message = "Successfully connected. Received ${readings.size} sensor reading(s).",
                    readings = readings,
                    rawJson = bodyString,
                    responseTimeMs = elapsed
                )
            } catch (e: IOException) {
                val elapsed = System.currentTimeMillis() - startTime
                Esp32PollResult(
                    success = false,
                    message = "Connection failed: ${e.localizedMessage ?: "Device unreachable at $ipAddress"}",
                    responseTimeMs = elapsed
                )
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                Esp32PollResult(
                    success = false,
                    message = "Error: ${e.localizedMessage ?: "Unknown parsing error"}",
                    responseTimeMs = elapsed
                )
            }
        }

    fun parseEsp32Json(jsonString: String): List<ParsedReading> {
        val result = mutableListOf<ParsedReading>()
        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)

                // Format 1: {"readings": [...]}
                if (obj.has("readings") && obj.get("readings") is JSONArray) {
                    val array = obj.getJSONArray("readings")
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val name = item.optString("name", "Sensor $i")
                        val type = item.optString("type", inferTypeFromName(name))
                        val value = item.optDouble("value", 0.0)
                        val unit = item.optString("unit", inferUnitFromType(type))
                        result.add(ParsedReading(name, type, value, unit))
                    }
                    return result
                }

                // Format 2: Single sensor object {"sensor": "temperature", "value": 24.5, "unit": "°C"}
                if (obj.has("sensor") && obj.has("value")) {
                    val name = obj.optString("sensor", "Sensor")
                    val type = obj.optString("type", inferTypeFromName(name))
                    val value = obj.optDouble("value", 0.0)
                    val unit = obj.optString("unit", inferUnitFromType(type))
                    result.add(ParsedReading(name, type, value, unit))
                    return result
                }

                // Format 3: Key-value map e.g. {"temperature": 25.1, "humidity": 60.5, "light": 340}
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val rawVal = obj.opt(key)
                    if (rawVal is Number) {
                        val type = inferTypeFromName(key)
                        val unit = inferUnitFromType(type)
                        val cleanName = key.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        result.add(ParsedReading(cleanName, type, rawVal.toDouble(), unit))
                    }
                }
            } else if (trimmed.startsWith("[")) {
                // Format 4: Direct array of readings
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val name = item.optString("name", "Sensor $i")
                    val type = item.optString("type", inferTypeFromName(name))
                    val value = item.optDouble("value", 0.0)
                    val unit = item.optString("unit", inferUnitFromType(type))
                    result.add(ParsedReading(name, type, value, unit))
                }
            }
        } catch (_: Exception) {
            // Return whatever was parsed
        }
        return result
    }

    private fun inferTypeFromName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("temp") -> "TEMPERATURE"
            lower.contains("hum") -> "HUMIDITY"
            lower.contains("soil") || lower.contains("moist") -> "SOIL_MOISTURE"
            lower.contains("light") || lower.contains("lux") || lower.contains("ldr") -> "LIGHT"
            lower.contains("press") || lower.contains("baro") || lower.contains("bmp") || lower.contains("bme") -> "PRESSURE"
            lower.contains("bat") || lower.contains("volt") -> "BATTERY"
            else -> "CUSTOM"
        }
    }

    private fun inferUnitFromType(type: String): String {
        return when (type) {
            "TEMPERATURE" -> "°C"
            "HUMIDITY", "SOIL_MOISTURE", "BATTERY" -> "%"
            "LIGHT" -> "lux"
            "PRESSURE" -> "hPa"
            else -> "units"
        }
    }
}
