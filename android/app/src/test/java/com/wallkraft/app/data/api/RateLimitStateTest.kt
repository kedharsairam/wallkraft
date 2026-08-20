package com.wallkraft.app.data.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimitStateTest {

    @After
    fun tearDown() {
        RateLimitState.reset()
    }

    @Test
    fun initial_isNotLimited() {
        RateLimitState.reset()
        assertFalse(RateLimitState.limited.value)
        assertEquals(45, RateLimitState.remaining.value)
    }

    @Test
    fun update_withRemainingPositive_isNotLimited() {
        RateLimitState.update(10)
        assertFalse(RateLimitState.limited.value)
        assertEquals(10, RateLimitState.remaining.value)
    }

    @Test
    fun update_withZero_isLimited() {
        RateLimitState.update(0)
        assertTrue(RateLimitState.limited.value)
        assertEquals(0, RateLimitState.remaining.value)
    }

    @Test
    fun reset_clearsLimitedAndRemaining() {
        RateLimitState.update(0)
        assertTrue(RateLimitState.limited.value)
        RateLimitState.reset()
        assertFalse(RateLimitState.limited.value)
        assertEquals(45, RateLimitState.remaining.value)
    }

    @Test
    fun update_cancelsPreviousCooldown() = runTest {
        // Update to 0 triggers cooldown job; then update to non-zero cancels it.
        RateLimitState.update(0)
        assertTrue(RateLimitState.limited.value)
        RateLimitState.update(5)
        assertFalse(RateLimitState.limited.value)
        assertEquals(5, RateLimitState.remaining.value)
    }
}
