package com.wallkraft.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wallkraft.app.core.design.KraftConstants

class MainActivity : ComponentActivity() {
    /**
     * Debug-only slow-frame watcher. Logs frames slower than ~2 vsyncs so
     * jank is visible in logcat during development. Never registered in
     * release builds — zero overhead, zero data collection.
     */
    private val frameListener =
        Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            val totalMs = metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000
            if (totalMs > KraftConstants.SlowFrameThresholdMs) {
                Log.w("WallKraftPerf", "slow frame ${totalMs}ms")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as WallKraftApplication).container

        // Dark mode always — paint window background black before content draws.
        window.decorView.setBackgroundColor(Color.BLACK)

        setContent {
            WallKraftApp(container)
        }
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG) {
            window.addOnFrameMetricsAvailableListener(
                frameListener,
                Handler(Looper.getMainLooper()),
            )
        }
    }

    override fun onPause() {
        if (BuildConfig.DEBUG) {
            window.removeOnFrameMetricsAvailableListener(frameListener)
        }
        super.onPause()
    }
}
