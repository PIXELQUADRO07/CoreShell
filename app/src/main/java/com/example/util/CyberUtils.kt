package com.example.util

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

object CyberUtils {

    /**
     * Generates a real RSA-2048 key pair.
     * Returns a Pair: (OpenSSH Public Key, PEM Private Key)
     */
    fun generateRsa2048KeyPair(): Pair<String, String> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateKey

        // Encode public key to OpenSSH format
        val sshPubKey = encodeOpenSSHPublicKey(publicKey)
        
        // Encode private key to PEM format
        val pemPrivKey = encodePKCS8PrivateKey(privateKey)

        return Pair(sshPubKey, pemPrivKey)
    }

    private fun encodeOpenSSHPublicKey(key: RSAPublicKey): String {
        val byteStream = ByteArrayOutputStream()
        val dataStream = DataOutputStream(byteStream)

        // Standard OpenSSH "ssh-rsa" format:
        // string "ssh-rsa"
        // mpint  exponent
        // mpint  modulus
        writeString(dataStream, "ssh-rsa")
        writeMpInt(dataStream, key.publicExponent)
        writeMpInt(dataStream, key.modulus)

        val base64Bytes = Base64.encodeToString(byteStream.toByteArray(), Base64.NO_WRAP)
        return "ssh-rsa $base64Bytes Cybershell-GeneratedKey"
    }

    private fun writeString(dos: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.US_ASCII)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun writeMpInt(dos: DataOutputStream, bigInt: java.math.BigInteger) {
        var bytes = bigInt.toByteArray()
        // If the first bit is set (positive integer, but bytes contains sign bit), 
        // a redundant zero byte might already be added by BigInteger.toByteArray().
        // Otherwise, standard mpint encoding expects it.
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun encodePKCS8PrivateKey(key: RSAPrivateKey): String {
        val encodedBytes = key.encoded
        val base64 = Base64.encodeToString(encodedBytes, Base64.DEFAULT)
        return """
-----BEGIN PRIVATE KEY-----
${base64.trim()}
-----END PRIVATE KEY-----
        """.trimIndent()
    }
}
