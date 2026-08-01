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
    Toplist("toplist"),
}

enum class Order(val value: String) {
    Desc("desc"),
    Asc("asc"),
}

/** Toplist time ranges (only meaningful when [Sorting.Toplist]). */
enum class TopRange(val value: String) {
    Day1("1d"),
    Days3("3d"),
    Week1("1w"),
    Month1("1M"),
    Months3("3M"),
    Months6("6M"),
    Year1("1y"),
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
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val sorting: Sorting = Sorting.DateAdded,
    val order: Order = Order.Desc,
    val topRange: TopRange? = null,
    val ratio: String? = null,
    val query: String = "",
)
