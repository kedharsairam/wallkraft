package com.wallkraft.app.data.rotation

import com.wallkraft.app.domain.model.RotationSchedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Boundary math for rotation schedules. Pure java.time — no Android
 * dependencies, JVM unit-tested.
 *
 * Rolling timers ("24h from whenever you tapped") surprise users; clock
 * boundaries don't: hourly fires at the top of each hour, daily around
 * midnight, weekly Monday morning. Exactness is bounded by Doze and the
 * battery constraint — hence "around", never "exactly". Each chain link
 * re-derives its delay from now, so drift self-corrects every run.
 */
object RotationTiming {

    /**
     * Milliseconds from [nowMs] to the next boundary for [schedule].
     * Exactly on a boundary yields 0 (run now). OFF yields 0 (unused).
     */
    fun initialDelayMs(schedule: RotationSchedule, nowMs: Long, zone: ZoneId): Long {
        if (schedule == RotationSchedule.OFF) return 0
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val boundaryMs = when (schedule) {
            RotationSchedule.HOURLY -> {
                val top = now.truncatedTo(ChronoUnit.HOURS)
                if (top.toInstant().toEpochMilli() < nowMs) top.plusHours(1) else top
            }
            RotationSchedule.DAILY -> {
                val midnight = now.truncatedTo(ChronoUnit.DAYS)
                if (midnight.toInstant().toEpochMilli() < nowMs) midnight.plusDays(1) else midnight
            }
            RotationSchedule.WEEKLY -> {
                val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .truncatedTo(ChronoUnit.DAYS)
                if (monday.toInstant().toEpochMilli() < nowMs) monday.plusWeeks(1) else monday
            }
            RotationSchedule.OFF -> return 0
        }.toInstant().toEpochMilli()
        return maxOf(0, boundaryMs - nowMs)
    }
}
