package com.wallkraft.app.data.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tracks Wallhaven's rate limit, authoritative from response headers.
 * Mirrors the Flutter app: the server's `X-RateLimit-Remaining` value wins,
 * no local consumption.
 *
 * When the limit is exhausted the app enters a short cooldown (Wallhaven
 * limits reset every minute), after which [limited] flips back to false so a
 * fresh request can succeed and re-read the header. Without this, a single
 * 429 would leave the app permanently "limited" until process restart,
 * because every retry fails fast before it can fetch a new header.
 */
object RateLimitState {
    private const val COOLDOWN_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cooldownJob: Job? = null

    private val _limited = MutableStateFlow(false)
    private val _remaining = MutableStateFlow(45)

    val limited: StateFlow<Boolean> = _limited.asStateFlow()
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    fun update(remaining: Int) {
        _remaining.value = remaining
        cooldownJob?.cancel()
        if (remaining > 0) {
            _limited.value = false
        } else {
            _limited.value = true
            // Auto-clear after the cooldown so browsing can resume without a
            // restart. Only the most recent 429 schedules the reset.
            cooldownJob = scope.launch {
                delay(COOLDOWN_MS)
                _limited.value = false
            }
        }
    }

    fun reset() {
        cooldownJob?.cancel()
        _remaining.value = 45
        _limited.value = false
    }
}
