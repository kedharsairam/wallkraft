package com.wallkraft.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Sorting
import com.wallkraft.app.domain.model.ThemeMode

/** Localized user-facing names. The enums keep their raw wire values for the API. */

@Composable
fun Sorting.displayName(): String = stringResource(
    when (this) {
        Sorting.DateAdded -> R.string.sorting_date_added
        Sorting.Relevance -> R.string.sorting_relevance
        Sorting.Random -> R.string.sorting_random
        Sorting.Views -> R.string.sorting_views
        Sorting.Favorites -> R.string.sorting_favorites
    },
)

@Composable
fun Orientation.displayName(): String = stringResource(
    when (this) {
        Orientation.Both -> R.string.orientation_both
        Orientation.Portrait -> R.string.orientation_portrait
        Orientation.Landscape -> R.string.orientation_landscape
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
