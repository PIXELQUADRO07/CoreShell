package com.example.coreshell.data.dao

import androidx.room.*
import com.example.coreshell.data.model.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY lastConnected DESC, createdAt DESC")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Int): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    @Update
    suspend fun updateServer(server: ServerEntity)

    @Delete
    suspend fun deleteServer(server: ServerEntity)

    @Query("UPDATE servers SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Int, timestamp: Long)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: Int)
}
