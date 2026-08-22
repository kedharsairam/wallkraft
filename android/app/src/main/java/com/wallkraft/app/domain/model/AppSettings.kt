package com.wallkraft.app.domain.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

/**
 * App settings, persisted via DataStore.
 *
 * [categories], [purity], [sorting], and [orientation] seed the browse screen's
 * initial filters; [apiKey] is an optional Wallhaven account key sent as
 * `X-API-Key` on every request. [dataSaverMode] defers full-res image
 * downloads until the user actually needs them (zoom / set / share) to save
 * mobile data. Defaults: All categories (111), SFW only — NSFW never.
 * Matches wallhaven.cc.
 */
data class AppSettings(
    val apiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.System,
    val categories: Set<Category> = setOf(Category.General, Category.Anime, Category.People),
    val purity: Set<Purity> = setOf(Purity.SfW),
    val sorting: Sorting = Sorting.DateAdded,
    val orientation: Orientation = Orientation.Both,
    val dataSaverMode: Boolean = false,
)
