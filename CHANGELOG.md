# Changelog

All notable changes to WallKraft will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-08-04

### Added
- **Downloads page** — new tab with `DownloadManager` integration: query, open, and remove downloaded wallpapers.
- **Wallpaper position selection** — `WallpaperPositionDialog` (Home / Lock / Both) via bottom action sheet before setting wallpaper.
- **Keyboard dismiss on scroll** — keyboard auto-hides when user starts scrolling the grid.

### Changed
- **Bottom tab bar redesigned** — outer `Scaffold(bottomBar)` for proper navigation bar inset handling; Apple-style tab bar with icon + label, 80dp height.
- **Search bar Apple-style** — `BasicTextField` with custom decoration box, 40dp height, filled gray pill, no border. Text proportionally balanced with container.
- **Search bar text** — placeholder and input use `labelMedium` (~13sp) for visual balance with tab bar labels.
- **Grid edge padding** — increased from 8dp to 16dp (`ScreenEdge`) for proper breathing room.
- **Wallpaper position dialog** — `AlertDialog` → `ModalBottomSheet` (Apple-style action sheet).
- **FilterSheet spacing** — hardcoded `8.dp` values replaced with `KraftSpacing.Spacing8` token.
- Domain models annotated with `@Immutable` — Compose can now skip recomposition when these pass through grids.

### Fixed
- **Bottom bar labels clipped** — navigation bar insets were being consumed by inner Scaffolds, leaving zero padding for the tab bar. Fixed by using outer `Scaffold(bottomBar)` and setting `contentWindowInsets = WindowInsets(0,0,0,0)` on all inner Scaffolds.
- **Inner screen background mismatch** — all inner Scaffolds now use `containerColor = background` to eliminate color strip between outer and inner Scaffolds.
- **ZoomableImage KDoc** — removed stale `[imageRatio]` parameter references.
- **DetailScreen Scaffold** — added missing `containerColor = background` for consistency.

### Removed
- **Share feature** — removed share button from top bar and fullscreen viewer; removed `WallpaperActions.share()`.

## [1.2.0] - 2026-08-04

### Fixed
- **Critical:** CancellationException in `setAsWallpaper` — was silently swallowed, now properly re-thrown for structured concurrency.
- **Critical:** Empty-URL crash when wallpaper path is blank — now shows error state instead of crashing.
- **Critical:** Unbounded in-memory cache (`cacheById`) — replaced `mutableMapOf` with `LruCache(200)` to prevent OOM on long sessions.
- Config-change data loss — added `android:configChanges` to prevent Activity recreation during wallpaper set.

### Security
- API key now encrypted at rest via `EncryptedSharedPreferences` (AES-256-GCM).
- CI workflow masks secrets with `::add-mask::` to prevent log exposure.
- CI signing fallback now fails hard instead of silently using debug key.
- ProGuard rules narrowed from `com.wallkraft.app.**` to specific API/domain model packages.

### Added
- Pull-to-refresh on browse grid (`PullToRefreshBox` + `viewModel::refresh`).
- 48dp touch targets on all action buttons (WCAG 2.5.5 compliance).
- ViewModel tests for `DetailViewModel` (5 tests), `FavoritesViewModel` (2 tests), `SettingsViewModel` (6 tests).
- `androidTest` dependencies (JUnit, Espresso, Compose UI test).
- `.editorconfig` for consistent formatting.
- `@Immutable` annotations on domain models (`Wallpaper`, `Thumbs`, `Tag`, `WallpaperMeta`, `WallpaperResponse`) for Compose skip optimization.
- `GridAppendFooter` extracted from inline lambda for better recomposition.

## [1.1.0] - 2026-08-03

### Added
- Real release signing with 30-year keystore.
- CI release workflow with keystore restore from GitHub Secrets.
- Play Store listing, privacy policy, screenshots.
- Version bumped to 1.1.0 (code 2).

### Fixed
- Build deprecation warnings (`autoCorrect` → `autoCorrectEnabled`, `Icons.Filled.OpenInNew` → `AutoMirrored`).
- Native theme flash on cold start (`setKeepOnScreenCondition` + runtime background color).

## [1.0.0] - 2026-08-02

### Added
- Initial release: Browse, Detail, Favorites, Settings.
- Wallhaven API integration with search and filters.
- Staggered grid with infinite scroll.
- Fullscreen viewer with zoom and pan.
- Local favorites with Room database.
- Settings: API key, theme, sorting, categories.
