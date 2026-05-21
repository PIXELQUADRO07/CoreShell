package com.example.data.repository

import com.example.data.db.RSAKeyPairDao
import com.example.data.db.ServerProfileDao
import com.example.data.model.RSAKeyPair
import com.example.data.model.ServerProfile
import kotlinx.coroutines.flow.Flow

class SshRepository(
    private val serverProfileDao: ServerProfileDao,
    private val rsaKeyPairDao: RSAKeyPairDao
) {
    val allProfiles: Flow<List<ServerProfile>> = serverProfileDao.getAllProfiles()
    val allKeyPairs: Flow<List<RSAKeyPair>> = rsaKeyPairDao.getAllKeyPairs()

    suspend fun getProfileById(id: String): ServerProfile? = serverProfileDao.getProfileById(id)
    suspend fun insertProfile(profile: ServerProfile) = serverProfileDao.insertProfile(profile)
    suspend fun updateProfile(profile: ServerProfile) = serverProfileDao.updateProfile(profile)
    suspend fun deleteProfile(profile: ServerProfile) = serverProfileDao.deleteProfile(profile)

    suspend fun getKeyPairById(id: String): RSAKeyPair? = rsaKeyPairDao.getKeyPairById(id)
    suspend fun insertKeyPair(keyPair: RSAKeyPair) = rsaKeyPairDao.insertKeyPair(keyPair)
    suspend fun deleteKeyPair(keyPair: RSAKeyPair) = rsaKeyPairDao.deleteKeyPair(keyPair)
}
