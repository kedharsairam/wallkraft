package com.wallkraft.app.data.rotation

import com.wallkraft.app.domain.model.RotationSchedule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationTimingTest {

    private val utc = ZoneId.of("UTC")

    private fun at(iso: String): Long = Instant.parse(iso).toEpochMilli()

    @Test
    fun hourly_waits_for_top_of_hour() {
        assertEquals(23 * 60_000L, RotationTiming.initialDelayMs(RotationSchedule.HOURLY, at("2026-09-08T14:37:00Z"), utc))
    }

    @Test
    fun hourly_exact_boundary_runs_now() {
        assertEquals(0L, RotationTiming.initialDelayMs(RotationSchedule.HOURLY, at("2026-09-08T14:00:00Z"), utc))
    }

    @Test
    fun daily_waits_for_midnight() {
        // 14:37 -> 00:00 = 9h23m.
        assertEquals((9 * 3_600 + 23 * 60) * 1_000L, RotationTiming.initialDelayMs(RotationSchedule.DAILY, at("2026-09-08T14:37:00Z"), utc))
    }

    @Test
    fun daily_exact_midnight_runs_now() {
        assertEquals(0L, RotationTiming.initialDelayMs(RotationSchedule.DAILY, at("2026-09-08T00:00:00Z"), utc))
    }

    @Test
    fun weekly_waits_for_monday_midnight() {
        // Tue 14:37 -> Mon 00:00 = 5d 9h23m.
        assertEquals((5 * 86_400 + 9 * 3_600 + 23 * 60) * 1_000L, RotationTiming.initialDelayMs(RotationSchedule.WEEKLY, at("2026-09-08T14:37:00Z"), utc))
    }

    @Test
    fun weekly_exact_monday_midnight_runs_now() {
        // 2026-09-07 was a Monday.
        assertEquals(0L, RotationTiming.initialDelayMs(RotationSchedule.WEEKLY, at("2026-09-07T00:00:00Z"), utc))
    }

    @Test
    fun weekly_monday_morning_waits_full_week() {
        // Mon 00:01 -> next Mon 00:00 = 6d 23h59m.
        assertEquals((6 * 86_400 + 23 * 3_600 + 59 * 60) * 1_000L, RotationTiming.initialDelayMs(RotationSchedule.WEEKLY, at("2026-09-07T00:01:00Z"), utc))
    }

    @Test
    fun off_has_no_delay() {
        assertEquals(0L, RotationTiming.initialDelayMs(RotationSchedule.OFF, at("2026-09-08T14:37:00Z"), utc))
    }
}
