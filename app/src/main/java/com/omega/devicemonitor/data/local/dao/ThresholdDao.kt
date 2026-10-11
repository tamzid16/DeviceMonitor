package com.omega.devicemonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.omega.devicemonitor.data.local.entity.ThresholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThresholdDao {
    @Query("SELECT * FROM thresholds ORDER BY sensorType ASC, sensorName ASC")
    fun getAllThresholds(): Flow<List<ThresholdEntity>>

    @Query("SELECT * FROM thresholds WHERE userId = :userId ORDER BY sensorType ASC, sensorName ASC")
    fun getThresholdsForUser(userId: Long): Flow<List<ThresholdEntity>>

    @Query("SELECT * FROM thresholds WHERE userId = :userId ORDER BY sensorType ASC, sensorName ASC")
    suspend fun getThresholdsForUserDirect(userId: Long): List<ThresholdEntity>

    @Query("SELECT * FROM thresholds WHERE userId = :userId AND enabled = 1")
    suspend fun getActiveThresholdsForUser(userId: Long): List<ThresholdEntity>

    @Query("SELECT * FROM thresholds WHERE enabled = 1")
    suspend fun getActiveThresholds(): List<ThresholdEntity>

    @Query("SELECT COUNT(*) FROM thresholds WHERE userId = :userId")
    fun getThresholdCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM thresholds WHERE userId = :userId")
    suspend fun getThresholdCountForUserDirect(userId: Long): Int

    @Query("SELECT COUNT(*) FROM thresholds")
    fun getThresholdCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM thresholds")
    suspend fun getThresholdCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreshold(threshold: ThresholdEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThresholds(thresholds: List<ThresholdEntity>): List<Long>

    @Update
    suspend fun updateThreshold(threshold: ThresholdEntity)

    @Delete
    suspend fun deleteThreshold(threshold: ThresholdEntity)

    @Query("DELETE FROM thresholds WHERE id = :id")
    suspend fun deleteThresholdById(id: Long)

    @Query("DELETE FROM thresholds WHERE userId = :userId")
    suspend fun deleteThresholdsForUser(userId: Long)

    @Query("DELETE FROM thresholds")
    suspend fun deleteAllThresholds()
}
