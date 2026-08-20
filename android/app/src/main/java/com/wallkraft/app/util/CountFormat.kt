package com.wallkraft.app.util

import java.util.Locale

/**
 * Compact count formatting for UI labels (Instagram-style): 41 -> "41",
 * 5,085 -> "5.1k", 1,234,567 -> "1.2m". Trailing ".0" is trimmed so round
 * numbers read cleanly ("1k", not "1.0k").
 */
fun formatCount(count: Int): String = when {
    count < 1_000 -> count.toString()
    count < 1_000_000 -> compact(count / 1_000.0, "k")
    else -> compact(count / 1_000_000.0, "m")
}

private fun compact(value: Double, suffix: String): String {
    val formatted = String.format(Locale.US, "%.1f", value)
    val trimmed = if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
    return "$trimmed$suffix"
}
