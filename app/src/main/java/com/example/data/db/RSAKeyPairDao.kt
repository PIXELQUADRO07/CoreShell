package com.example.data.db

import androidx.room.*
import com.example.data.model.RSAKeyPair
import kotlinx.coroutines.flow.Flow

@Dao
interface RSAKeyPairDao {
    @Query("SELECT * FROM rsa_key_pairs ORDER BY createdAt DESC")
    fun getAllKeyPairs(): Flow<List<RSAKeyPair>>

    @Query("SELECT * FROM rsa_key_pairs WHERE id = :id LIMIT 1")
    suspend fun getKeyPairById(id: String): RSAKeyPair?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyPair(keyPair: RSAKeyPair)

    @Delete
    suspend fun deleteKeyPair(keyPair: RSAKeyPair)
}
