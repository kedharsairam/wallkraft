package com.wallkraft.app.util

/**
 * Pure search-history list logic — no Android dependencies, JVM unit-tested.
 *
 * History is most-recent-first, de-duplicated (case-insensitive, keeping the
 * latest casing), and bounded. Persistence lives in [SearchHistoryStore].
 */
object SearchHistory {

    const val MAX_ENTRIES = 15

    /** Separator for the single-string DataStore representation. Unit separator: never typed. */
    private const val SEPARATOR = ''

    fun add(history: List<String>, query: String, max: Int = MAX_ENTRIES): List<String> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return history
        return (listOf(cleaned) + history.filterNot { it.equals(cleaned, ignoreCase = true) })
            .take(max)
    }

    fun serialize(history: List<String>): String =
        history.joinToString(SEPARATOR.toString())

    fun deserialize(raw: String): List<String> =
        if (raw.isEmpty()) emptyList()
        else raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_ENTRIES)
}
