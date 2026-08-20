package com.wallkraft.app.domain.model

/** Wallhaven search categories. Bit values follow the API's category mask. */
enum class Category(val value: String) {
    General("general"),
    Anime("anime"),
    People("people"),
}

enum class Sorting(val value: String) {
    DateAdded("date_added"),
    Relevance("relevance"),
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

/** Wallhaven purity — never NSFW (001). Only 100 (SFW) or 110 (SFW+sketchy). */
enum class Purity(val apiValue: String) {
    SfW("100"),
    SfWSketchy("110"),
}

/**
 * Search filters for the Wallhaven API.
 *
 * Defaults: General only (100), SFW only (100), newest first.
 * NSFW is never requested. Sketchy is opt-in via [purity].
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General),
    val sorting: Sorting = Sorting.DateAdded,
    val orientation: Orientation = Orientation.Both,
    val query: String = "",
    val purity: Purity = Purity.SfW,
)