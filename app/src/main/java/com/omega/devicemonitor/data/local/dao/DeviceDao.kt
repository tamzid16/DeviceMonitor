package com.omega.devicemonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY name ASC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE userId = :userId ORDER BY name ASC")
    fun getDevicesForUser(userId: Long): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE userId = :userId ORDER BY name ASC")
    suspend fun getDevicesForUserDirect(userId: Long): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Long): DeviceEntity?

    @Query("SELECT COUNT(*) FROM devices WHERE userId = :userId")
    fun getDeviceCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices WHERE userId = :userId")
    suspend fun getDeviceCountForUserDirect(userId: Long): Int

    @Query("SELECT COUNT(*) FROM devices")
    fun getDeviceCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun getDeviceCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>): List<Long>

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("DELETE FROM devices WHERE userId = :userId")
    suspend fun deleteDevicesForUser(userId: Long)

    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()
}
