package com.wallkraft.app.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the pure mapping in [ConnectivityObserver].
 *
 * The NetworkCallback itself needs a device; only the capability-to-boolean
 * mapping is unit-tested here.
 */
class ConnectivityObserverTest {

    @Test
    fun `null capabilities means offline`() {
        assertFalse(ConnectivityObserver.isOnline(null))
    }

    @Test
    fun `resolveIsOnline requires both network and internet capability`() {
        assertTrue(ConnectivityObserver.resolveIsOnline(hasNetwork = true, hasInternetCapability = true))
        assertFalse(ConnectivityObserver.resolveIsOnline(hasNetwork = false, hasInternetCapability = true))
        assertFalse(ConnectivityObserver.resolveIsOnline(hasNetwork = true, hasInternetCapability = false))
        assertFalse(ConnectivityObserver.resolveIsOnline(hasNetwork = false, hasInternetCapability = false))
    }
}
