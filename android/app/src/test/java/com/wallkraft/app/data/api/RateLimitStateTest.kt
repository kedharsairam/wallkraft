package com.wallkraft.app.data.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RateLimitStateTest {

    private lateinit var rateLimitState: RateLimitState

    @Before
    fun setUp() {
        rateLimitState = RateLimitState()
    }

    @After
    fun tearDown() {
        rateLimitState.reset()
    }

    @Test
    fun initial_isNotLimited() {
        rateLimitState.reset()
        assertFalse(rateLimitState.limited.value)
        assertEquals(45, rateLimitState.remaining.value)
    }

    @Test
    fun update_withRemainingPositive_isNotLimited() {
        rateLimitState.update(10)
        assertFalse(rateLimitState.limited.value)
        assertEquals(10, rateLimitState.remaining.value)
    }

    @Test
    fun update_withZero_isLimited() {
        rateLimitState.update(0)
        assertTrue(rateLimitState.limited.value)
        assertEquals(0, rateLimitState.remaining.value)
    }

    @Test
    fun reset_clearsLimitedAndRemaining() {
        rateLimitState.update(0)
        assertTrue(rateLimitState.limited.value)
        rateLimitState.reset()
        assertFalse(rateLimitState.limited.value)
        assertEquals(45, rateLimitState.remaining.value)
    }

    @Test
    fun update_cancelsPreviousCooldown() = runTest {
        // Update to 0 triggers cooldown job; then update to non-zero cancels it.
        rateLimitState.update(0)
        assertTrue(rateLimitState.limited.value)
        rateLimitState.update(5)
        assertFalse(rateLimitState.limited.value)
        assertEquals(5, rateLimitState.remaining.value)
    }
}
