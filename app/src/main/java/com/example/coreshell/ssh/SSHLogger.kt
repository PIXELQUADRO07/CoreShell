package com.example.coreshell.ssh

import android.util.Log

/**
 * Secure logger for SSH activities.
 * Automatically filters passwords and sensitive information.
 */
object SSHLogger {
    private const val TAG = "SSH_LOG"

    fun i(message: String) {
        Log.i(TAG, sanitize(message))
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, sanitize(message), throwable)
    }

    fun d(message: String) {
        Log.d(TAG, sanitize(message))
    }

    /**
     * Removes or masks potential sensitive information from logs.
     */
    private fun sanitize(message: String): String {
        // Example: mask password parameters in commands or URLs
        return message.replace(Regex("(?i)password=[^&\\s]+"), "password=********")
            .replace(Regex("(?i)passphrase=[^&\\s]+"), "passphrase=********")
    }
}
