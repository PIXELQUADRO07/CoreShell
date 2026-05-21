package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "server_profiles")
data class ServerProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authType: String = "PASSWORD", // "PASSWORD" or "RSA_KEY"
    val password: String = "",
    val rsaKeyId: String? = null,
    val neonColorHex: String = "#00FFD1", // Accent color: Cyan, Pink, Yellow, Green, etc.
    val isTailscale: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
