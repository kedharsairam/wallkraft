# Changelog

All notable changes to WallKraft will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Removed
- **Windows app discontinued** — WallKraft is now Android-only. The `windows/` directory (Rust + Slint), Windows CI/release workflows, NSIS installer, winget manifests, and all Windows documentation have been removed. Future releases ship only `WallKraft-<version>-android-arm64.apk`.

## [1.10.0] - 2026-08-20

### Changed
- **Detail bottom panel redesigned** — the collapsed bar now shows only three elements: a drag handle, the uploader row (avatar + name), and a "More details" pull hint. Stat pills and tags are hidden until the panel is expanded, giving the collapsed state a clean, minimal look.
- **Panel anchored to screen bottom** — the bottom nav bar is hidden on the detail screen so the panel sits flush with the device's bottom edge. The panel grows upward as it expands.
- **Gesture bar clearance** — the collapsed panel height now accounts for the gesture navigation bar, so the "More details" hint always sits above the white pill and never overlaps it.

### Fixed
- **Panel content misaligned on open** — AnimatedVisibility's internal Box was center-aligning oversized children, pushing the handle/uploader/hint ~257px above the visible clip region. Replaced with `animateFloatAsState` + `graphicsLayer` alpha fade, eliminating the internal layout entirely.
- **Collapsed content clipped incorrectly** — Box also center-aligns children taller than itself, even with `contentAlignment = TopStart`. Replaced the inner measurement Box with a custom `Layout` composable that measures content at full panel height but places it at y=0, ensuring the handle/uploader/hint always render at the top of the panel.
- **Stats peeking in collapsed state** — the live panel was hardcoding `collapsed = false`, so stat pills and tags were always rendered and only hidden by the clip region. Now passes the actual collapsed state so stats/tags are not rendered at all when collapsed.
- **Collapse-offset logic removed** — the old `bottomOffsetPx` / `collapseInsetPx` system that shifted the collapsed bar up by the bottom inset is gone. The panel is now simply anchored to the bottom bar at all times.

## [1.9.0] - 2026-08-14

### Changed
- **Unified versioning** — WallKraft now uses one version number across all platforms. Android and Windows are both at 1.9.0; a `v*` tag releases both platforms together. Release workflows refuse (fail CI) if a tag doesn't match the platform version, so a one-sided bump can't ship a partial release.
- **Windows app joins the shared version** — the Windows app moves from its own 0.x track onto the unified 1.9.0 version (no feature change; version alignment only).

## [0.1.0] - 2026-08-13

### Added — Windows app (release-ready)
- **Installer** — per-user NSIS build (`WallKraft-<version>-setup.exe`) installing to `%LOCALAPPDATA%\Programs\WallKraft` with Start Menu shortcuts, Add/Remove Programs entry, and silent install/uninstall support.
- **App icon & metadata** — Kraft-brand icon embedded in the exe and window/taskbar, full VERSIONINFO, PerMonitorV2 DPI manifest.
- **Auto-update** — checks GitHub releases on startup (fail-silent, offline-first) and applies new versions in place: download installer, restart, silent install, relaunch.
- **Drag & drop save** — drag any grid tile to the desktop or a folder to save the full-resolution image via native OLE.
- **First-load skeletons** — shimmer placeholders that mirror the real masonry column count.
- **winget manifests** — generated and validated (`tools/publish-winget.ps1`), ready for submission to winget-pkgs.

### Changed
- **Platform version tracks** — Android (1.x) and Windows (0.x) now version independently; the `v*` tag triggers each platform's release only when the tag matches its own version.

## [1.8.0] - 2026-08-11

### Internal
- **Fully automated dual-platform releases** — a single version tag now builds and uploads both the Android APK and the Windows EXE to one release page, with no manual steps.

## [1.7.0] - 2026-08-11

### Added
- **Windows app (preview)** — WallKraft now ships for Windows as a native Rust + Slint build. Browse the Wallhaven library, search, view details, and set the desktop wallpaper directly from the app. A single self-contained executable, no installation required.
- **Monorepo restructure** — the repository now hosts both platforms: `android/` (Kotlin + Compose) and `windows/` (Rust + Slint), with per-platform CI and a shared release pipeline.

### Changed
- **Release artifacts carry platform and architecture** — files are now named `WallKraft-<version>-android-arm64.apk` and `WallKraft-<version>-windows-x64.exe` so each download is unambiguous.

## [1.6.2] - 2026-08-11

### Added
- **Apple-style crop dialog redesign** — the Set-wallpaper dialog now uses a clean translucent panel (no blur): a hard-edged bottom sheet with rounded top corners that extends behind the gesture nav bar, a thin top scrim for status-bar legibility, a close button on a dark circle top-left, and an Apple-style segmented control (white pill with black text) for the home / lock / both position picker.

### Changed
- **Set-as-wallpaper reuses Coil's in-flight full-res load** — the manual OkHttp download fallback is gone. When the full-res isn't cached yet, the app now issues a Coil request identical to the detail screen's (same data, decoded at original size, on the shared loader), so Coil joins an already-running download instead of starting a second one. Data saver still defers the full-res until you zoom, set, or share — the explicit Set action downloads on demand as before.
- **Share benefits from the same path** — sharing a non-favorite wallpaper also reuses Coil's disk cache or joins the in-flight load instead of a separate OkHttp download.
- **Dead code removed** — the unused OkHttp-based `setAsWallpaper` overload and `downloadToCache` are deleted; `imageFile`/`share` no longer take an `OkHttpClient`.

## [1.6.1] - 2026-08-10

### Fixed
- **Crop dialog image off-center** — the dialog window is full-screen but its content is inset below the status bar, so the crop surface rendered ~121px lower than it should. The image surface is now offset back up by the status bar height, centering the wallpaper on the visible screen and making the crop match what the user sees.
- **Double-tap fill in crop dialog** — double-tapping now zooms the image to fill the screen top-to-bottom (edge-to-edge, behind the system bars) and toggles back to the centered fit, mirroring the detail screen behavior.
- **Crop dialog title clipped into the status bar** — the status-bar offset is applied only to the image surface, so the title and controls stay below the status bar.
- **Crop dialog bottom buttons at the screen edge** — bottom padding now accounts for both the navigation bar and the status bar inset, keeping the buttons above the gesture nav bar.

## [1.6.0] - 2026-08-09

### Added
- **Downloads tab restored** — a proper file library: lists downloaded wallpapers with size, opens the file location, and deletes files. Replaces the removed v1.5.0 tab.
- **Batch delete** — select multiple (or all) downloads and delete them with a single confirmation instead of confirming each file.
- **Share** — share the actual image file (offline favorite copy, else downloaded to cache) via a new `FileProvider`, falling back to the wallhaven.cc URL.
- **Set wallpaper with crop & position** — full-screen crop dialog with pinch-zoom and drag to frame the visible region, position chips (home / lock / both), then crops to screen resolution and applies via `WallpaperManager`.
- **Data saver mode** — opt-in Settings toggle. When ON, the detail screen shows the thumbnail instantly and defers the full-res download until you zoom; local files (offline favorites) still load immediately. Grid tap-to-prefetch is gated too.
- **Offline favorites** — full-res image downloaded to app-private storage when favorited, deleted on unfavorite; favorites viewable with no internet.
- **Search response caching** — file-backed, 30-minute TTL, bounded (100 entries), offline fallback; returning to a visited Browse screen is instant and works offline.
- **Shared image cache** — Coil disk cache (512 MB) shared between grid and detail loaders so thumbnails fetched by the grid are reused by detail (and vice-versa).
- **Network retry with backoff** — transient failures (network errors + 5xx) retried up to 3 times with exponential backoff; never retries rate-limit, client errors, or parse failures.
- **Haptics** — subtle vibration on favorite toggle, download, and successful wallpaper set.
- **Hindi localization** — `values-hi/strings.xml` with all strings translated, proving the i18n architecture.

### Changed
- **Tag browsing reworked** — tapping a tag now opens the Browse screen pre-filtered to that tag (shared `WallpaperListViewModel`), replacing the dedicated Tag screen.
- **Pull-to-refresh fixed** — `search()` gained a `forceRefresh` param that bypasses the cache, and `refresh()` enforces a minimum 500ms so the spinner always animates away.
- **Accessibility** — all icons have content descriptions, touch targets are 48dp, hardcoded strings moved to resources.

### Fixed
- **Blank Downloads/Favorites content** — the inner screens ignored the Scaffold's `innerPadding`, so the list was laid out behind the opaque top bar. Now padded correctly.
- **Duplicate `categories` query parameter** in the Wallhaven API search call.
- **Unit tests updated** — 29 tests covering the new data saver setting and reworked Browse flow.

## [1.5.0] - 2026-08-07

### Added
- **Tag browsing** — tapping a tag on the detail screen opens a dedicated grid of wallpapers sharing that tag.
- **Instant detail preview** — the grid passes its thumbnail and full-res path to the detail screen so the image renders immediately while full metadata loads in the background.
- **Orientation filter** — replaced the old sort order/toplist options with a Portrait / Landscape / Both orientation filter (maps to Wallhaven's `ratios` parameter).
- **Smooth fling & prefetching** — custom fling behavior and a debounced image prefetch keep the browse grid scrolling smoothly.

### Changed
- **Browse screen rewritten** — inline filter dropdown (replaces the old FilterSheet), smoother grid, lighter tiles.
- **Detail screen rewritten** — removed the hero transition; uniform fade/scale open-close animation; determinate top loading bar; zoom now hides the bottom bar for an edge-to-edge view.
- **Grid tiles simplified** — removed the category dot and favorites overlay for faster rendering.
- **Downloads tab removed** — the Downloads screen is no longer part of the app.

### Fixed
- **Unit tests updated** for the removed `Order`/`Toplist`/`TopRange` enums and the new `Orientation` filter.

## [1.4.0] - 2026-08-05

### Added
- **Downloaded indicator** — green badge on wallpapers that have been downloaded to the device, visible in Browse and Favorites grids.
- **Unit tests** — 28 tests covering BrowseViewModel, FavoritesViewModel, SettingsViewModel, DetailViewModel, and FavoriteDao (Room).

### Changed
- **FullscreenViewer extracted** — immersive fullscreen viewer moved to its own file (`FullscreenViewer.kt`) for maintainability.
- **Material3 NavigationBar** — replaced hand-built Row+Column bottom bar with `NavigationBar`+`NavigationBarItem` for screen reader semantics and accessibility.
- **Wallpaper caching** — `setAsWallpaper()` checks Downloads folder and internal cache before re-downloading.
- **Downloads content type** — file extension detected from URL instead of hardcoded `.jpg`.
- **Room schema export** — enabled for database migration testing.
- **Filter dropdown animation** — custom slide-in/fade transition replacing DropdownMenu for smoother dropdown feel.

### Fixed
- **Filter dropdown positioning** — menu now sits flush below the search bar instead of being offset by ~580px.
- **BrowseViewModel pagination error** — failures now surface to UI instead of being silently swallowed.
- **SettingsViewModel scope leak** — `appScope` cancelled in `onCleared()`.
- **ShimmerGrid allocation** — removed per-recomposition `Random`; uses precomputed heights.
- **Downloads formatSize** — fixed truncation; proper locale formatting.

## [1.3.0] - 2026-08-04

## [1.3.0] - 2026-08-04

### Added
- **Downloads page** — new tab with `DownloadManager` integration: query, open, and remove downloaded wallpapers.
- **Wallpaper position selection** — `WallpaperPositionDialog` (Home / Lock / Both) via bottom action sheet before setting wallpaper.
- **Keyboard dismiss on scroll** — keyboard auto-hides when user starts scrolling the grid.

### Changed
- **Bottom tab bar redesigned** — outer `Scaffold(bottomBar)` for proper navigation bar inset handling; clean tab bar with icon + label, 80dp height.
- **Search bar** — `BasicTextField` with custom decoration box, 40dp height, filled gray pill, no border. Text proportionally balanced with container.
- **Search bar text** — placeholder and input use `labelMedium` (~13sp) for visual balance with tab bar labels.
- **Grid edge padding** — increased from 8dp to 16dp (`ScreenEdge`) for proper breathing room.
- **Wallpaper position dialog** — `AlertDialog` → `ModalBottomSheet` (action sheet).
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
