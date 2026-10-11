package com.omega.devicemonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReadingsForUser(userId: Long): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getReadingsForUserDirect(userId: Long): List<SensorReadingEntity>

    @Query("SELECT * FROM sensor_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getReadingsForDevice(deviceId: Long): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE deviceId = :deviceId AND sensorName = :sensorName ORDER BY timestamp DESC")
    fun getReadingsForSensor(deviceId: Long, sensorName: String): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE deviceId = :deviceId AND sensorName = :sensorName AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getReadingsForSensorSince(deviceId: Long, sensorName: String, sinceTimestamp: Long): Flow<List<SensorReadingEntity>>

    @Query("SELECT * FROM sensor_readings WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadingsForUser(userId: Long, limit: Int = 60): Flow<List<SensorReadingEntity>>

    @Query("SELECT COUNT(*) FROM sensor_readings WHERE userId = :userId")
    fun getReadingCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sensor_readings WHERE userId = :userId")
    suspend fun getReadingCountForUserDirect(userId: Long): Int

    @Query("SELECT COUNT(*) FROM sensor_readings")
    fun getReadingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sensor_readings")
    suspend fun getReadingCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<SensorReadingEntity>): List<Long>

    @Delete
    suspend fun deleteReading(reading: SensorReadingEntity)

    @Query("DELETE FROM sensor_readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)

    @Query("DELETE FROM sensor_readings WHERE deviceId = :deviceId AND sensorName = :sensorName")
    suspend fun deleteReadingsForSensor(deviceId: Long, sensorName: String)

    @Query("DELETE FROM sensor_readings WHERE userId = :userId")
    suspend fun deleteReadingsForUser(userId: Long)

    @Query("DELETE FROM sensor_readings")
    suspend fun deleteAllReadings()
}
