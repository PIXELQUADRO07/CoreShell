package com.example.data.db

import androidx.room.*
import com.example.data.model.ServerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerProfileDao {
    @Query("SELECT * FROM server_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<ServerProfile>>

    @Query("SELECT * FROM server_profiles ORDER BY createdAt DESC")
    fun getAllProfilesSync(): List<ServerProfile>

    @Query("SELECT * FROM server_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ServerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ServerProfile)

    @Update
    suspend fun updateProfile(profile: ServerProfile)

    @Delete
    suspend fun deleteProfile(profile: ServerProfile)
}
