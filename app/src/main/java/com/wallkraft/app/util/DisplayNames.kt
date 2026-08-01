package com.wallkraft.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Order
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode
import com.wallkraft.app.domain.model.TopRange

/** Localized user-facing names. The enums keep their raw wire values for the API. */

@Composable
fun Sorting.displayName(): String = stringResource(
    when (this) {
        Sorting.DateAdded -> R.string.sorting_date_added
        Sorting.Relevance -> R.string.sorting_relevance
        Sorting.Random -> R.string.sorting_random
        Sorting.Views -> R.string.sorting_views
        Sorting.Favorites -> R.string.sorting_favorites
        Sorting.Toplist -> R.string.sorting_toplist
    },
)

@Composable
fun TopRange.displayName(): String = stringResource(
    when (this) {
        TopRange.Day1 -> R.string.top_range_1d
        TopRange.Days3 -> R.string.top_range_3d
        TopRange.Week1 -> R.string.top_range_1w
        TopRange.Month1 -> R.string.top_range_1m
        TopRange.Months3 -> R.string.top_range_3m
        TopRange.Months6 -> R.string.top_range_6m
        TopRange.Year1 -> R.string.top_range_1y
    },
)

@Composable
fun Category.displayName(): String = stringResource(
    when (this) {
        Category.General -> R.string.category_general
        Category.Anime -> R.string.category_anime
        Category.People -> R.string.category_people
    },
)

@Composable
fun Order.displayName(): String = stringResource(
    when (this) {
        Order.Desc -> R.string.order_desc
        Order.Asc -> R.string.order_asc
    },
)

@Composable
fun ThemeMode.displayName(): String = stringResource(
    when (this) {
        ThemeMode.System -> R.string.theme_system
        ThemeMode.Light -> R.string.theme_light
        ThemeMode.Dark -> R.string.theme_dark
    },
)

/** Maps a raw Wallhaven category value ("anime") to its localized label. */
@Composable
fun wallpaperCategoryLabel(value: String): String {
    val category = Category.entries.firstOrNull { it.value == value }
    return category?.displayName() ?: value.replaceFirstChar { it.uppercase() }
}
