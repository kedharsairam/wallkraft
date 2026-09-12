package com.wallkraft.app.core.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Haptics table — one helper, no more universal LongPress.
 * Maps Kraft intent → Compose HapticFeedbackType. DESIGN.md §5
 * selectionChanged -> TextHandleMove (light), button -> TextHandleMove,
 * destructive -> LongPress (medium), sheet snap -> LongPress
 * Success/error use LongPress + semantic (Compose has no CONFIRM/REJECT).
 */
object KraftHaptics {
    fun selectionChanged(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun buttonPress(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun destructive(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.LongPress)
    fun sheetSnap(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.LongPress)
    fun success(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.LongPress)
    fun error(h: HapticFeedback) = h.performHapticFeedback(HapticFeedbackType.LongPress)
    // Keep LongPress as destructive until platform CONFIRM is wired via View.
}
