package com.wallkraft.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.TopRange
import com.wallkraft.app.domain.model.WallhavenFilters
import com.wallkraft.app.domain.model.toCategoryParam
import com.wallkraft.app.domain.model.toPurityParam

/**
 * A user-saved Browse filter set ("Blue anime", "Toplist month", …).
 *
 * Filter dimensions are explicit columns — not a filtersJson blob — so rows
 * stay queryable and survive code-level renames of [WallhavenFilters] fields.
 * [categories] and [purity] are API bitmasks ("100"); [sorting], [topRange]
 * and [orientation] store the API values ("toplist", "1M", "portrait", with
 * "both" for [Orientation.Both] which omits the `ratios` param).
 * Names are unique (case-insensitive resolve) — saving a duplicate overwrites
 * the filters and resolves to the existing row.
 */
@Entity(
    tableName = "saved_searches",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["lastUsedAt"]),
    ],
)
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String = "",
    val categories: String,
    val purity: String,
    val sorting: String,
    val topRange: String = "1M",
    val colors: String = "",
    val orientation: String = "both",
    val createdAt: Long,
    val lastUsedAt: Long,
    val useCount: Int = 0,
) {
    fun toFilters(): WallhavenFilters = WallhavenFilters(
        categories = categories.toCategorySet(),
        sorting = sorting.toSorting(),
        topRange = topRange.toTopRange(),
        orientation = orientation.toOrientation(),
        query = query,
        purity = purity.toPuritySet(),
        colors = colors,
    )

    companion object {
        fun fromFilters(
            name: String,
            filters: WallhavenFilters,
            now: Long,
        ): SavedSearchEntity = SavedSearchEntity(
            name = name,
            query = filters.query,
            categories = filters.categories.toCategoryParam(),
            purity = filters.purity.toPurityParam(),
            sorting = filters.sorting.value,
            topRange = filters.topRange.value,
            colors = filters.colors,
            orientation = filters.orientation.value.ifBlank { ORIENTATION_BOTH },
            createdAt = now,
            lastUsedAt = now,
        )
    }
}

/** Stored sentinel for [Orientation.Both] (which sends no `ratios` param). */
const val ORIENTATION_BOTH = "both"

private fun String.toCategorySet(): Set<Category> {
    if (length != 3) return setOf(Category.General, Category.Anime, Category.People)
    return buildSet {
        if (this@toCategorySet[0] == '1') add(Category.General)
        if (this@toCategorySet[1] == '1') add(Category.Anime)
        if (this@toCategorySet[2] == '1') add(Category.People)
    }
}

private fun String.toPuritySet(): Set<Purity> {
    if (length != 3) return setOf(Purity.SFW)
    return buildSet {
        if (this@toPuritySet[0] == '1') add(Purity.SFW)
        if (this@toPuritySet[1] == '1') add(Purity.Sketchy)
        if (this@toPuritySet[2] == '1') add(Purity.NSFW)
    }
}

private fun String.toSorting(): Sorting =
    Sorting.entries.firstOrNull { it.value == this || it.name == this } ?: Sorting.DateAdded

private fun String.toTopRange(): TopRange {
    // Toplist rows predating topRange (or with a blank/unknown value) fall
    // back to Month — the API default and the app's default.
    if (isBlank()) return TopRange.Month
    return TopRange.entries.firstOrNull { it.value == this || it.name == this } ?: TopRange.Month
}

private fun String.toOrientation(): Orientation = when (lowercase()) {
    "", ORIENTATION_BOTH, Orientation.Both.name.lowercase() -> Orientation.Both
    Orientation.Portrait.value, Orientation.Portrait.name.lowercase() -> Orientation.Portrait
    Orientation.Landscape.value, Orientation.Landscape.name.lowercase() -> Orientation.Landscape
    else -> Orientation.Both
}
