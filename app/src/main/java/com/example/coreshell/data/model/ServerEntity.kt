package com.example.coreshell.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nickname: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String,
    val authType: String = "password",
    val privateKeyPath: String? = null,
    val savedCommands: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnected: Long? = null
)
