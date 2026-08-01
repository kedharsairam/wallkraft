package com.wallkraft.app.domain.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

/**
 * User settings, persisted via DataStore.
 *
 * [categories], [sorting], and [order] seed the browse screen's initial
 * filters; [apiKey] is an optional Wallhaven account key sent as `X-API-Key`
 * on every request. Purity is deliberately not a setting — Wallkraft is
 * strictly SFW-only.
 */
data class AppSettings(
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.System,
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val sorting: Sorting = Sorting.DateAdded,
    val order: Order = Order.Desc,
)
