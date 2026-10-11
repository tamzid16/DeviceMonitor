package com.omega.devicemonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.omega.devicemonitor.data.local.dao.AlertLogDao
import com.omega.devicemonitor.data.local.dao.DeviceDao
import com.omega.devicemonitor.data.local.dao.SensorReadingDao
import com.omega.devicemonitor.data.local.dao.ThresholdDao
import com.omega.devicemonitor.data.local.dao.UserDao
import com.omega.devicemonitor.data.local.entity.AlertLogEntity
import com.omega.devicemonitor.data.local.entity.DeviceEntity
import com.omega.devicemonitor.data.local.entity.SensorReadingEntity
import com.omega.devicemonitor.data.local.entity.ThresholdEntity
import com.omega.devicemonitor.data.local.entity.UserProfileEntity

@Database(
    entities = [
        DeviceEntity::class,
        SensorReadingEntity::class,
        ThresholdEntity::class,
        AlertLogEntity::class,
        UserProfileEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DeviceMonitorDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun thresholdDao(): ThresholdDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: DeviceMonitorDatabase? = null

        fun getDatabase(context: Context): DeviceMonitorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeviceMonitorDatabase::class.java,
                    "device_monitor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
