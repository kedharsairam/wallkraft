package com.wallkraft.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped connectivity status backed by [ConnectivityManager.NetworkCallback].
 *
 * v1 exposes a simple online/offline boolean; extend later with metered/unmetered
 * if data-saver logic needs it.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = currentlyOnline()
        }

        override fun onLost(network: Network) {
            _isOnline.value = currentlyOnline()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isOnline.value = isOnline(capabilities)
        }
    }

    init {
        // registerDefaultNetworkCallback requires API 24+; minSdk is 26.
        connectivityManager?.registerDefaultNetworkCallback(callback)
    }

    private fun currentlyOnline(): Boolean {
        val manager = connectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        return isOnline(manager.getNetworkCapabilities(network))
    }

    companion object {
        /** Pure mapping — null capabilities means offline. */
        fun isOnline(capabilities: NetworkCapabilities?): Boolean =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        /** JVM-testable overload without Android framework types. */
        fun resolveIsOnline(hasNetwork: Boolean, hasInternetCapability: Boolean): Boolean =
            hasNetwork && hasInternetCapability
    }
}
