package com.omega.devicemonitor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omega.devicemonitor.data.local.DeviceMonitorDatabase
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.local.entity.UserProfileEntity
import com.omega.devicemonitor.data.network.Esp32ApiClient
import com.omega.devicemonitor.data.repository.DeviceMonitorRepository
import com.omega.devicemonitor.util.SecurityHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceMonitorRepositoryTest {

  private lateinit var db: DeviceMonitorDatabase
  private lateinit var repository: DeviceMonitorRepository

  @Before
  fun setupDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, DeviceMonitorDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = DeviceMonitorRepository(db)
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DeviceMonitor", appName)
  }

  @Test
  fun `registration stores a verifiable password hash`() = runBlocking {
    val result = repository.registerUser(
      name = "Alice Tech",
      email = "alice@devicemonitor.test",
      password = "securePassword123",
      avatarIndex = 1
    )
    assertTrue(result.isSuccess)
    val user = result.getOrNull()
    assertNotNull(user)
    assertEquals("Alice Tech", user!!.name)
    assertEquals("alice@devicemonitor.test", user.email)
    assertTrue(SecurityHelper.verifyPassword("securePassword123", user.passwordHash))
    assertFalse(SecurityHelper.verifyPassword("wrongPassword", user.passwordHash))

    // Verify login
    val loginSuccess = repository.loginUser("alice@devicemonitor.test", "securePassword123")
    assertTrue(loginSuccess.isSuccess)

    val loginFail = repository.loginUser("alice@devicemonitor.test", "badPass")
    assertTrue(loginFail.isFailure)
  }


  @Test
  fun `legacy password hashes are upgraded after successful login`() = runBlocking {
    val password = "legacyPassword123"
    val legacyInput = "DeviceMonitor_Omega_Salt_2026:$password"
    val legacyHash = java.security.MessageDigest.getInstance("SHA-256")
      .digest(legacyInput.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
    val userId = db.userDao().insertUser(
      UserProfileEntity(
        name = "Legacy User",
        email = "legacy@test.com",
        passwordHash = legacyHash
      )
    )

    val login = repository.loginUser("legacy@test.com", password)

    assertTrue(login.isSuccess)
    val upgraded = db.userDao().getUserById(userId)
    assertNotNull(upgraded)
    assertTrue(upgraded!!.passwordHash.startsWith("pbkdf2_sha256\$"))
  }

  @Test
  fun `monitoring data is isolated between accounts`() = runBlocking {
    val u1 = repository.registerUser("User One", "user1@test.com", "pass123").getOrThrow()
    val u2 = repository.registerUser("User Two", "user2@test.com", "pass456").getOrThrow()

    val d1 = db.deviceDao().insertDevice(
      DeviceEntity(userId = u1.id, name = "ESP32 User1 Node", location = "Office", ipAddress = "192.168.1.10", deviceType = "ESP32")
    )
    val d2 = db.deviceDao().insertDevice(
      DeviceEntity(userId = u2.id, name = "ESP32 User2 Node", location = "Garage", ipAddress = "192.168.1.20", deviceType = "ESP32")
    )

    val u1Devices = db.deviceDao().getDevicesForUserDirect(u1.id)
    val u2Devices = db.deviceDao().getDevicesForUserDirect(u2.id)

    assertTrue(u1Devices.any { it.id == d1 })
    assertFalse(u1Devices.any { it.id == d2 })

    assertTrue(u2Devices.any { it.id == d2 })
    assertFalse(u2Devices.any { it.id == d1 })
  }

  @Test
  fun `JSON backup can be exported and restored`() = runBlocking {
    val user = repository.registerUser("Export User", "export@test.com", "pass123").getOrThrow()
    val exportedJson = repository.exportUserDataAsJson(user.id)
    assertTrue(exportedJson.contains("DeviceMonitor"))
    assertTrue(exportedJson.contains("export@test.com"))

    val restoreUser = repository.registerUser("Restore User", "restore@test.com", "pass456").getOrThrow()
    val importResult = repository.importUserDataFromJson(restoreUser.id, exportedJson)
    assertTrue(importResult.isSuccess)
  }

  @Test
  fun `ESP32 parser reads an array of sensor readings`() {
    val client = Esp32ApiClient()
    val sampleJson = """
      {
        "readings": [
          {"name": "DHT22 Temp", "type": "TEMPERATURE", "value": 24.5, "unit": "°C"},
          {"name": "Soil Sensor", "type": "SOIL_MOISTURE", "value": 45.0, "unit": "%"}
        ]
      }
    """.trimIndent()

    val parsed = client.parseEsp32Json(sampleJson)
    assertEquals(2, parsed.size)
    assertEquals("DHT22 Temp", parsed[0].sensorName)
    assertEquals("TEMPERATURE", parsed[0].sensorType)
    assertEquals(24.5, parsed[0].value, 0.01)
  }

  @Test
  fun `ESP32 parser reads a flat sensor map`() {
    val client = Esp32ApiClient()
    val sampleJson = """
      {
        "temperature": 26.2,
        "humidity": 60.5,
        "soil_moisture": 35.0
      }
    """.trimIndent()

    val parsed = client.parseEsp32Json(sampleJson)
    assertTrue(parsed.isNotEmpty())
    val temp = parsed.find { it.sensorType == "TEMPERATURE" }
    assertTrue(temp != null)
    assertEquals(26.2, temp!!.value, 0.01)
  }
}
