package com.omega.devicemonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.omega.devicemonitor.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserProfileEntity?

    @Query("SELECT COUNT(*) FROM user_profiles")
    fun getUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getUserCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserProfileEntity): Long

    @Update
    suspend fun updateUser(user: UserProfileEntity)

    @Delete
    suspend fun deleteUser(user: UserProfileEntity)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllUsers()
}
