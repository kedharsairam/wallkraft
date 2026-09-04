package com.wallkraft.app.data.api

import com.wallkraft.app.core.design.KraftConstants
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
    private const val COOLDOWN_MS = KraftConstants.RateLimitCooldownMs

    // Uses applicationScope when available to avoid leaking a process-wide scope;
    // falls back to a default scope for unit tests where Application is not present.
    private var appScope: CoroutineScope? = null
    private val scope: CoroutineScope get() = appScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cooldownJob: Job? = null

    /** Called from WallKraftApplication to bind the process-wide scope. */
    fun attachScope(scope: CoroutineScope) {
        appScope = scope
    }

    private val _limited = MutableStateFlow(false)
    private val _remaining = MutableStateFlow(KraftConstants.RateLimitDefaultRemaining)

    val limited: StateFlow<Boolean> = _limited.asStateFlow()
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    @Synchronized
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

    @Synchronized
    fun reset() {
        cooldownJob?.cancel()
        _remaining.value = KraftConstants.RateLimitDefaultRemaining
        _limited.value = false
    }
}
