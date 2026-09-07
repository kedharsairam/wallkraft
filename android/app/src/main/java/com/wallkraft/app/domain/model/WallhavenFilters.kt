package com.wallkraft.app.domain.model

/** Wallhaven search categories. Bit values follow the API's category mask. */
enum class Category(val value: String) {
    General("general"),
    Anime("anime"),
    People("people"),
}

enum class Sorting(val value: String) {
    Relevance("relevance"),
    Random("random"),
    DateAdded("date_added"),
    Views("views"),
    Favorites("favorites"),
    Toplist("toplist"),
    Hot("hot"),
}

/**
 * Toplist time range. Maps to the Wallhaven `topRange` query parameter, which
 * is only sent when [Sorting] is [Sorting.Toplist] (values follow the API:
 * 1d, 1w, 1M, 6M, 1y — the ranges wallhaven.cc offers).
 */
enum class TopRange(val value: String) {
    Day("1d"),
    Week("1w"),
    Month("1M"),
    SixMonths("6M"),
    Year("1y"),
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
 * Wallhaven purity — SFW, Sketchy, and NSFW.
 * NSFW requires a valid API key to access.
 */
enum class Purity {
    SFW,
    Sketchy,
    NSFW,
}

fun Set<Purity>.toPurityParam(): String = buildString {
    append(if (contains(Purity.SFW)) '1' else '0')
    append(if (contains(Purity.Sketchy)) '1' else '0')
    append(if (contains(Purity.NSFW)) '1' else '0')
}

/**
 * Search filters for the Wallhaven API.
 *
 * Defaults: All categories (111), SFW only (100), newest first. Matches wallhaven.cc.
 * NSFW requires a valid API key — it's gated at the UI level and stripped if no key is set.
 * Purity is multi-select like categories: SFW, Sketchy, or NSFW.
 * [colors] is a 6-digit hex (e.g. "0000ff") sent as the API `colors` parameter;
 * blank means no color filtering.
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val sorting: Sorting = Sorting.DateAdded,
    val topRange: TopRange = TopRange.Month,
    val orientation: Orientation = Orientation.Both,
    val query: String = "",
    val purity: Set<Purity> = setOf(Purity.SFW),
    val colors: String = "",
)