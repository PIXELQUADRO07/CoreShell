package com.example.coreshell.data.repository

import android.content.Context
import com.example.coreshell.data.AppDatabase
import com.example.coreshell.data.model.ServerEntity
import com.example.coreshell.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ServerRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).serverDao()
    private val cryptoManager = CryptoManager()

    fun getAllServers(): Flow<List<ServerEntity>> = dao.getAllServers().map { servers ->
        servers.map { decryptServer(it) }
    }

    suspend fun getServerById(id: Int): ServerEntity? = withContext(Dispatchers.IO) {
        dao.getServerById(id)?.let { decryptServer(it) }
    }

    suspend fun saveServer(server: ServerEntity): Long = withContext(Dispatchers.IO) {
        dao.insertServer(encryptServer(server))
    }

    suspend fun updateServer(server: ServerEntity) = withContext(Dispatchers.IO) {
        dao.updateServer(encryptServer(server))
    }

    suspend fun deleteServer(server: ServerEntity) = withContext(Dispatchers.IO) {
        dao.deleteServer(server)
    }

    suspend fun deleteServerById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun updateLastConnected(id: Int) = withContext(Dispatchers.IO) {
        dao.updateLastConnected(id, System.currentTimeMillis())
    }

    private fun encryptServer(server: ServerEntity): ServerEntity {
        return server.copy(
            password = cryptoManager.encrypt(server.password),
            privateKeyPath = server.privateKeyPath?.let { cryptoManager.encrypt(it) }
        )
    }

    private fun decryptServer(server: ServerEntity): ServerEntity {
        return server.copy(
            password = cryptoManager.decrypt(server.password),
            privateKeyPath = server.privateKeyPath?.let { cryptoManager.decrypt(it) }
        )
    }
}
