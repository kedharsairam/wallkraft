# Changelog

All notable changes to WallKraft will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

### Changed
- Domain models annotated with `@Immutable` — Compose can now skip recomposition when these pass through grids.

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
