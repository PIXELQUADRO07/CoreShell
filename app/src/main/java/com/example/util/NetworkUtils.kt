package com.example.util

import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    
    /**
     * Checks if a Tailscale or standard VPN interface is currently active on the device,
     * or if any network interface holds a Tailscale CGNAT IP address (100.64.0.0/10).
     */
    fun isTailscaleVpnActive(): Boolean {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            interfaces.any { networkInterface ->
                if (!networkInterface.isUp || networkInterface.isLoopback) return@any false
                
                val nameLower = networkInterface.name.lowercase()
                val isVpnName = nameLower.contains("tun") || nameLower.contains("vpn") || nameLower.contains("tailscale")
                
                val hasTailscaleIp = Collections.list(networkInterface.inetAddresses).any { inetAddress ->
                    val ip = inetAddress.hostAddress ?: ""
                    ip.startsWith("100.") // Tailscale CGNAT subnet (100.64.0.0/10) starts with 100.x
                }
                
                isVpnName || hasTailscaleIp
            }
        } catch (e: Exception) {
            false
        }
    }
}
