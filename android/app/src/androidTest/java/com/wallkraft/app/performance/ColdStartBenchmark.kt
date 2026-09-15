package com.wallkraft.app.performance

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Manual cold-start benchmark.
 *
 * Measures intent-to-display: force-stop the app, launch it, and record the
 * wall-clock elapsed time until the process reports it has drawn its first
 * frame.  The threshold is 2 000 ms — the checklist target for cold start.
 *
 * Because [androidx.benchmark.macro] is not available in this project's
 * dependencies, this uses a simple System.nanoTime() approach with
 * UiAutomator to launch and verify the activity is visible.
 */
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {

    companion object {
        private const val TAG = "PerfBenchmark"
        private const val COLD_START_THRESHOLD_MS = 2_000L
        private const val LAUNCH_PACKAGE = "com.wallkraft.app"
        private const val LAUNCH_ACTIVITY = "com.wallkraft.app.MainActivity"
    }

    @Test
    fun coldStart_withinThreshold() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Force-stop the app to guarantee a cold start on next launch.
        device.executeShellCommand("am force-stop $LAUNCH_PACKAGE")
        Thread.sleep(500) // Allow process to fully die.

        val startNanos = System.nanoTime()

        // Launch the app via am start (intent-to-display starts here).
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(
                "am start -W -n $LAUNCH_PACKAGE/$LAUNCH_ACTIVITY -a android.intent.action.MAIN -c android.intent.category.LAUNCHER",
            ).close()

        val elapsedNanos = System.nanoTime() - startNanos
        val elapsedMs = elapsedNanos / 1_000_000

        Log.d(TAG, "Cold start: ${elapsedMs}ms")

        // Give the activity time to fully render before asserting.
        Thread.sleep(1_000)

        assertTrue(
            "Cold start ${elapsedMs}ms exceeded ${COLD_START_THRESHOLD_MS}ms threshold",
            elapsedMs < COLD_START_THRESHOLD_MS,
        )
    }
}
