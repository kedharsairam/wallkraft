package com.wallkraft.app.domain.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

/**
 * User settings, persisted via DataStore.
 *
 * [categories], [purity], [sorting], and [order] seed the browse screen's
 * initial filters; [apiKey] is an optional Wallhaven account key sent as
 * `X-API-Key` on every request.
 */
data class AppSettings(
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.System,
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val purity: Set<Purity> = setOf(Purity.Sfw),
    val sorting: Sorting = Sorting.DateAdded,
    val order: Order = Order.Desc,
)
