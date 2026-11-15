package com.example.mobile.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

internal data class NetworkState(val available: Boolean, val generation: Long = 0)

internal class NetworkMonitor(context: Context, scope: CoroutineScope) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    val state = callbackFlow {
        var current: Network? = null
        var generation = 0L
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                current = network
                trySend(NetworkState(true, ++generation))
            }

            override fun onLost(network: Network) {
                if (current == network) {
                    current = null
                    trySend(NetworkState(false, ++generation))
                }
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.stateIn(scope, SharingStarted.Eagerly, NetworkState(manager.activeNetwork != null))
}