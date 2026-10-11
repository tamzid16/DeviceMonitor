package com.omega.devicemonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.omega.devicemonitor.data.local.entity.AlertLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertLogDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertLogEntity>>

    @Query("SELECT * FROM alert_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAlertsForUser(userId: Long): Flow<List<AlertLogEntity>>

    @Query("SELECT * FROM alert_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAlertsForUserDirect(userId: Long): List<AlertLogEntity>

    @Query("SELECT * FROM alert_logs WHERE userId = :userId AND acknowledged = 0 ORDER BY timestamp DESC")
    fun getUnacknowledgedAlertsForUser(userId: Long): Flow<List<AlertLogEntity>>

    @Query("SELECT COUNT(*) FROM alert_logs WHERE userId = :userId AND acknowledged = 0")
    fun getUnacknowledgedCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM alert_logs WHERE userId = :userId")
    fun getTotalAlertCountForUser(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM alert_logs WHERE userId = :userId")
    suspend fun getTotalAlertCountForUserDirect(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertLogEntity>): List<Long>

    @Update
    suspend fun updateAlert(alert: AlertLogEntity)

    @Query("UPDATE alert_logs SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeAlert(id: Long)

    @Query("UPDATE alert_logs SET acknowledged = 1 WHERE userId = :userId")
    suspend fun acknowledgeAllAlertsForUser(userId: Long)

    @Delete
    suspend fun deleteAlert(alert: AlertLogEntity)

    @Query("DELETE FROM alert_logs WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM alert_logs WHERE userId = :userId")
    suspend fun deleteAlertsForUser(userId: Long)

    @Query("DELETE FROM alert_logs")
    suspend fun deleteAllAlerts()
}
