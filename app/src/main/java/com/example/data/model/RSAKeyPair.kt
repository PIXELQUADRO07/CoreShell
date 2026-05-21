package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "rsa_key_pairs")
data class RSAKeyPair(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val alias: String,
    val publicKey: String,    // Standard OpenSSH public key format (ssh-rsa ...)
    val privateKey: String,   // PEM private key format
    val createdAt: Long = System.currentTimeMillis()
)
