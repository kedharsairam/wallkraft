/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
package com.wallkraft.app.core.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Kraft-grade Reduce Motion: when user disables animations (Animator duration scale 0
 * or Accessibility Reduce Motion), all non-essential motion collapses to opacity/snap.
 * DESIGN.md §5 "Respect reduced motion" — keep opacity and blur, drop spring/scale.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) { isReduceMotionEnabled(context) }
}

fun isReduceMotionEnabled(context: Context): Boolean {
    // 1) Animator duration scale 0 = "Remove animations" in Developer Options
    try {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) return true
    } catch (_: Exception) { }
    // 2) AccessibilityManager.isReduceMotionEnabled() — Android 12+ (API 31)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (am != null) {
                val method = am.javaClass.getMethod("isReduceMotionEnabled")
                if (method.invoke(am) as? Boolean == true) return true
            }
        } catch (_: Exception) { }
    }
    return false
}
