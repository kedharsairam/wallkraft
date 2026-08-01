package com.wallkraft.app.domain.model

/** Wallhaven search categories. Bit values follow the API's category mask. */
enum class Category(val value: String) {
    General("general"),
    Anime("anime"),
    People("people"),
}

/** Wallhaven purity levels. Bit values follow the API's purity mask. */
enum class Purity(val value: String) {
    Sfw("sfw"),
    Sketchy("sketchy"),
    Nsfw("nsfw"),
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

fun Set<Purity>.toPurityParam(): String = buildString {
    append(if (contains(Purity.Sfw)) '1' else '0')
    append(if (contains(Purity.Sketchy)) '1' else '0')
    append(if (contains(Purity.Nsfw)) '1' else '0')
}

/**
 * Search filters for the Wallhaven API.
 *
 * The default matches the Wallkraft Flutter behavior: all categories, SFW only,
 * sorted by newest first.
 */
data class WallhavenFilters(
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val purity: Set<Purity> = setOf(Purity.Sfw),
    val sorting: Sorting = Sorting.DateAdded,
    val order: Order = Order.Desc,
    val topRange: TopRange? = null,
    val ratio: String? = null,
    val query: String = "",
)
