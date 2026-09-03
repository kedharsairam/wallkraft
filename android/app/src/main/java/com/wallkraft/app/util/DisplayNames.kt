package com.wallkraft.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wallkraft.app.R
import com.wallkraft.app.domain.model.Category
import com.wallkraft.app.domain.model.Orientation
import com.wallkraft.app.domain.model.Purity
import com.wallkraft.app.domain.model.Sorting

/** Localized user-facing names. The enums keep their raw wire values for the API. */

/** Returns the localized display name for this sorting option. */
@Composable
fun Sorting.displayName(): String = stringResource(
    when (this) {
        Sorting.DateAdded -> R.string.sorting_date_added
        Sorting.Hot -> R.string.sorting_hot
        Sorting.Random -> R.string.sorting_random
        Sorting.Views -> R.string.sorting_views
        Sorting.Favorites -> R.string.sorting_favorites
    },
)

/** Returns the localized display name for this orientation. */
@Composable
fun Orientation.displayName(): String = stringResource(
    when (this) {
        Orientation.Both -> R.string.orientation_both
        Orientation.Portrait -> R.string.orientation_portrait
        Orientation.Landscape -> R.string.orientation_landscape
    },
)

/** Returns the localized display name for this category. */
@Composable
fun Category.displayName(): String = stringResource(
    when (this) {
        Category.General -> R.string.category_general
        Category.Anime -> R.string.category_anime
        Category.People -> R.string.category_people
    },
)

/** Returns the localized display name for this purity level. */
@Composable
fun Purity.displayName(): String = stringResource(
    when (this) {
        Purity.SFW -> R.string.purity_sfw
        Purity.Sketchy -> R.string.purity_sketchy
        Purity.NSFW -> R.string.purity_nsfw
    },
)

/** Maps a raw Wallhaven category value (e.g. "anime") to its localized label. */
@Composable
fun wallpaperCategoryLabel(value: String): String {
    val category = Category.entries.firstOrNull { it.value == value }
    return category?.displayName() ?: value.replaceFirstChar { it.uppercase() }
}
