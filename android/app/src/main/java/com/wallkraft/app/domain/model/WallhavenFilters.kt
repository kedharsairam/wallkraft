package com.wallkraft.app.domain.model

/** Wallhaven search categories. Bit values follow the API's category mask. */
enum class Category(val value: String) {
    General("general"),
    Anime("anime"),
    People("people"),
}

enum class Sorting(val value: String) {
    DateAdded("date_added"),
    Hot("toplist"),
    Random("random"),
    Views("views"),
    Favorites("favorites"),
}

/**
 * Image orientation filter. Maps to the Wallhaven `ratios` query parameter:
 * [Both] omits the parameter (all orientations), [Portrait] and [Landscape]
 * narrow the results to that orientation.
 */
enum class Orientation(val value: String) {
    Both(""),
    Portrait("portrait"),
    Landscape("landscape"),
}

fun Set<Category>.toCategoryParam(): String = buildString {
    append(if (contains(Category.General)) '1' else '0')
    append(if (contains(Category.Anime)) '1' else '0')
    append(if (contains(Category.People)) '1' else '0')
}

/** Wallhaven purity — SFW and/or Sketchy; NSFW (001) is never requested. */
enum class Purity {
    SfW,
    Sketchy,
}

fun Set<Purity>.toPurityParam(): String = buildString {
    append(if (contains(Purity.SfW)) '1' else '0')
    append(if (contains(Purity.Sketchy)) '1' else '0')
    append('0')
}

/**
 * Search filters for the Wallhaven API.
 *
 * Defaults: All categories (111), SFW only (100), newest first. Matches wallhaven.cc.
 * NSFW is never requested (third char always 0).
 * Purity is multi-select like categories: SFW, Sketchy, or both.
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val sorting: Sorting = Sorting.DateAdded,
    val orientation: Orientation = Orientation.Both,
    val query: String = "",
    val purity: Set<Purity> = setOf(Purity.SfW),
)