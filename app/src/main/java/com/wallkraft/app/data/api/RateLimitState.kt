package com.wallkraft.app.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks Wallhaven's rate limit, authoritative from response headers.
 * Mirrors the Flutter app: the server's `X-RateLimit-Remaining` value wins,
 * no local consumption.
 */
object RateLimitState {
    private val _limited = MutableStateFlow(false)
    private val _remaining = MutableStateFlow(45)

    val limited: StateFlow<Boolean> = _limited.asStateFlow()
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    fun update(remaining: Int) {
        _remaining.value = remaining
        _limited.value = remaining <= 0
    }

    fun reset() {
        _remaining.value = 45
        _limited.value = false
    }
}
