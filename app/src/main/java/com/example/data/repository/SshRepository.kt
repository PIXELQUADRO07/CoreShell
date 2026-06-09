package com.example.data.repository

import com.example.data.db.RSAKeyPairDao
import com.example.data.db.ServerProfileDao
import com.example.data.model.RSAKeyPair
import com.example.data.model.ServerProfile
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SshRepository(
    private val serverProfileDao: ServerProfileDao,
    private val rsaKeyPairDao: RSAKeyPairDao
) {
    val allProfiles: Flow<List<ServerProfile>> = serverProfileDao.getAllProfiles().map { list ->
        list.map { it.copy(password = SecurityUtils.decrypt(it.password)) }
    }
    val allKeyPairs: Flow<List<RSAKeyPair>> = rsaKeyPairDao.getAllKeyPairs().map { list ->
        list.map { it.copy(privateKey = SecurityUtils.decrypt(it.privateKey)) }
    }

    suspend fun getProfileById(id: String): ServerProfile? = serverProfileDao.getProfileById(id)?.let {
        it.copy(password = SecurityUtils.decrypt(it.password))
    }
    suspend fun insertProfile(profile: ServerProfile) = serverProfileDao.insertProfile(
        profile.copy(password = SecurityUtils.encrypt(profile.password))
    )
    suspend fun updateProfile(profile: ServerProfile) = serverProfileDao.updateProfile(
        profile.copy(password = SecurityUtils.encrypt(profile.password))
    )
    suspend fun deleteProfile(profile: ServerProfile) = serverProfileDao.deleteProfile(profile)

    suspend fun getKeyPairById(id: String): RSAKeyPair? = rsaKeyPairDao.getKeyPairById(id)?.let {
        it.copy(privateKey = SecurityUtils.decrypt(it.privateKey))
    }
    suspend fun insertKeyPair(keyPair: RSAKeyPair) = rsaKeyPairDao.insertKeyPair(
        keyPair.copy(privateKey = SecurityUtils.encrypt(keyPair.privateKey))
    )
    suspend fun deleteKeyPair(keyPair: RSAKeyPair) = rsaKeyPairDao.deleteKeyPair(keyPair)
}

