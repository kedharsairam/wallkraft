package com.wallkraft.app.util

/**
 * Abstraction over elapsed-time measurement. Production uses
 * [android.os.SystemClock.elapsedRealtime]; tests inject a fake for deterministic control.
 */
fun interface ElapsedClock {
    /** Milliseconds since some arbitrary epoch (monotonic, non-decreasing). */
    fun elapsedMs(): Long
}
