package com.academia.shared.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic network connectivity monitoring.
 * Android: ConnectivityManager
 * iOS: NWPathMonitor (future)
 */
expect class NetworkMonitor {
    /**
     * Observe network connectivity changes.
     */
    fun observeConnectivity(): Flow<NetworkStatus>
    
    /**
     * Check current network status (one-time).
     */
    suspend fun isConnected(): Boolean
    
    /**
     * Get current network type.
     */
    suspend fun getNetworkType(): NetworkType
}

/**
 * Network connectivity status.
 */
enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

/**
 * Network connection type.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE,
    UNKNOWN
}
