package com.wallkraft.app.core.utils

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.view.accessibility.AccessibilityManager

/**
 * Apple-grade Reduce Motion: when user disables animations (Animator duration scale 0
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
    // 2) AccessibilityManager — some OEMs expose reduce motion as animation scale 0
    // Fallback: no system Reduce Motion toggle on stock Android; animator scale is the gate.
    return false
}
