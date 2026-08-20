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

/**
 * Search filters for the Wallhaven API.
 *
 * The default matches the Wallkraft Flutter behavior: all categories, sorted
 * by newest first. Purity is intentionally absent — Wallkraft is strictly
 * SFW-only (enforced in the API client and the repository).
 * [color] is a hex color without # (e.g. "ff0000") for Wallhaven's colors param.
 * [atleast] is a minimum resolution like "1920x1080" for Wallhaven's atleast param.
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val sorting: Sorting = Sorting.DateAdded,
    val orientation: Orientation = Orientation.Both,
    val query: String = "",
    val color: String? = null,
    val atleast: String? = null,
)