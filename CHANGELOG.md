# Changelog

All notable changes to WallKraft will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.17.0] - 2026-09-04

### Fixed
- **Thread-safety bug** — `SearchResponseCache` used a shared `MessageDigest` instance that was not thread-safe. Switched to `ThreadLocal` to prevent corrupted cache hashes on concurrent access.
- **Secret logging** — API keys and response bodies were logged to logcat in `WallhavenApi` and `SettingsViewModel`. Removed all sensitive data from logs.
- **Rate limit race condition** — `RateLimitState.update()` and `reset()` now use `@Synchronized` to prevent concurrent callers from corrupting cooldown state.
- **Error swallowing** — `WallpaperRepositoryImpl` now rethrows `Error` types (OOM, StackOverflow) instead of silently catching them.
- **Crop dialog spinner** — "Setting wallpaper…" text now shown below the spinner during wallpaper application.
- **Double data load** — `BrowseScreen` loaded downloaded IDs twice on first visit. Removed redundant `LaunchedEffect`.
- **Image loader init** — `GridImageLoader.init()` moved from `WallKraftApp` composable (called every recomposition) to `Application.onCreate` (called once).

### Changed
- **Network security** — Added `network_security_config.xml` enforcing HTTPS for all traffic. Added `android:usesCleartextTraffic="false"` to manifest.
- **Chip colors extracted** — Shared `ChipColors.kt` eliminates duplicate chip color definitions across `SearchFilterBar` and `SettingsScreen`.
- **Crop dialog dedup** — Extracted `applyAtPosition()` function, removing 84 lines of copy-pasted code across 3 position options.
- **RateLimitBanner** — Removed redundant `AnimatedVisibility` wrapper (parent controls composition).
- **Release build hardening** — Added `isDebuggable = false` to release buildType.

## [1.16.2] - 2026-09-04

### Fixed
- **API key validation** — rewrote `validateApiKey` to use `X-API-Key` header (consistent with search requests), wrapped in `withContext(Dispatchers.IO)` to prevent main thread blocking, and added `.trim()` to handle pasted keys with whitespace. Validation now works reliably.
- **NSFW locked chip color mismatch** — all filter chips now use explicit `containerColor = surfaceVariant` and `labelColor = onSurfaceVariant` for the inactive state. Chips look identical across the filter panel and Settings screen regardless of parent background.
- **NSFW locked chip locked appearance** — removed red tint from disabled state. Locked chip uses neutral `surfaceVariant` background, only shows red when unlocked and selected.
- **Set as Wallpaper back button z-order** — back button now renders after the dim overlay so it stays fully visible and tappable when the position picker popup is open.

### Changed
- **Set as Wallpaper screen** — bottom panel removed entirely. Back button (top-left) + checkmark button (top-right) in glass circles. Checkmark opens position picker popup (Home, Lock, Both) with dim overlay. Removed crop hint text and top gradient scrim. Window background set to black to prevent flash.

## [1.16.1] - 2026-09-04

### Changed
- **Filter chip label contrast** — all selected filter chip labels (categories, sorting, orientation, purity) now use white text for maximum readability on colored containers.
- **Purity borders on wallpaper cards** — Sketchy images show a 2dp orange border, NSFW images show a 2dp red border. Matches Wallhaven's visual language.
- **Purity border timing** — border is applied to the AsyncImage (shared element) so it stays visible during the detail-to-grid return transition.

### Fixed
- **Purity enum mapping** — `Wallpaper.purityEnum` now correctly maps `"nsfw"` to `Purity.NSFW` (was falling through to SFW).
- **API key verifying state** — `isValidating` is set immediately in `setApiKey()` so the UI shows "Verifying..." from the first frame instead of briefly flashing invalid.
- **API key status text** — simplified to show just the validity label without the key prefix.

### Removed
- **Apple references** — all "Apple HIG" mentions removed from codebase comments.

## [1.16.0] - 2026-09-04

### Added
- **NSFW purity filter** — new purity tier behind API key wall. Locked pill with lock icon shown when no valid API key. Unlocks automatically when valid key is entered. Uses `AuroraRed` accent.
- **API key validation** — validates against Wallhaven API using `X-API-Key` header. Shows green "Valid key — NSFW unlocked" or red "Invalid key — check and try again" status in Settings.
- **API key verifying state** — shows "Verifying..." loading indicator while the validation API call is in flight, instead of briefly flashing invalid.
- **API key validation state** — `apiKeyValid` persisted in settings, re-validated on key change and on app clear.
- **NSFW filter support in API** — `toPurityParam()` emits `1` for NSFW, repository adds `"nsfw"` to allowed purity set when selected.

### Changed
- **Dark-only theme** — light theme fully removed. `ThemeMode` enum deleted, `isSystemInDarkTheme()` checks removed, `LightColorScheme` removed. Single dark color scheme throughout.
- **Filter chip colors updated** — `ChipSelectedContainer` changed from steel blue to `AuroraBlue.copy(alpha = 0.2f)`, `ChipSelectedLabel` changed to `AuroraBlue`. Purity chips: SFW=Green@20%, Sketchy=Orange@20%, NSFW=Red@20%.
- **Panel backgrounds** — SearchFilterBar and KraftTopBar now use `SurfaceSecondary` (#2C2C2E) for visual hierarchy. Filter button uses `Surface` (#1C1C1E) to stand out.
- **Detail panel stats** — `StatPill` replaced with `StatItem` (icon + text vertical list). Icons: AspectRatio, Storage, Visibility, Favorite. All white on glass.
- **NSFW locked pill appearance** — reduced container alpha from 40% to 15% for subtler look.
- **NSFW stripped when invalid** — purity set automatically drops NSFW when API key is blank or invalid.
- **SettingsViewModel accepts `WallhavenApi`** — validation runs on 500ms debounce and on `onCleared()`.

### Fixed
- **API key validation always showing invalid** — `SettingsStore.update()` was overriding `apiKeyValid` to `false` whenever the key changed, discarding the validation result. Now always persists the validated value.
- **NSFW results filtered client-side** — `WallpaperRepositoryImpl` was stripping NSFW wallpapers from API response even when user selected NSFW purity. Fixed allowed purity set logic.
- **SettingsStore `apiKeyValid` reset** — removed incorrect reset-to-false on key change; the validation result from `SettingsViewModel` is now always respected.
- **API key flash invalid then valid** — added `isValidating` state to `SettingsViewModel` and "Verifying..." UI in Settings. Now shows a neutral loading state during the API call instead of briefly flashing invalid before flipping to valid.

### Removed
- **Light theme** — `ThemeMode`, `lightColorScheme()`, `isSystemInDarkTheme()` conditionals. Single dark theme only.

## [1.15.0] - 2026-09-03

### Added
- **App icon** — new launcher icon with adaptive icon support (foreground/background layers, 66dp safe zone). Black background, properly masked for circle/squircle/rounded-square launchers.
- **Splash screen** — updated to use new icon on black background, matching the app's design language.
- **Buy Me a Coffee** — support section in Settings with centered BMC button and left-aligned description. Opens `buymeacoffee.com/kedhartech`.
- **Privacy policy** — in-app dialog showing full privacy policy. No external link needed.
- **Developer credit** — About section now shows developer avatar (from GitHub), name, and title.
- **Open-source licenses** — link to LICENSE file in About section.
- **PRIVACY.md** — full privacy policy added to repo root.

### Changed
- **Settings > About** — reworked: developer avatar + name at top, version, GitHub, privacy policy, licenses. Removed tap-to-copy version gimmick.
- **Filter chip colors centralized** — `ChipSelectedContainer`, `ChipSelectedLabel`, purity colors moved to `KraftColors` for single-source management.
- **Filter panel dismiss** — fixed with `PointerEventPass.Initial` on Column. Also dismisses on focus loss and search bar tap.
- **Filter panel max height** — dynamic: `screenHeightDp - SearchBarHeight - 48dp`.
- **Typography fixes** — `labelLarge.copy(fontSize = Footnote)` replaced with `labelMedium` in SettingsGroup title and FilterSectionLabel.
- **Tab bar icon size** — uses `KraftIconSize.TabBar` token instead of hardcoded 25.dp.
- **Cache size refresh** — `DisposableEffect` with `ON_START` lifecycle observer.
- **Cache clear confirmation** — AlertDialog with Cancel/Clear buttons.
- **Data saver switch colors** — uses `MaterialTheme.colorScheme.onPrimary`/`.primary` instead of hardcoded.
- **API key max length** — 64 characters enforced.
- **Version tap-to-copy** — removed (useless for users).
- **README** — rewritten with download badge, BMC support section, updated privacy link.

### Fixed
- **Filter panel swipe dismiss** — `pointerInput` with `PointerEventPass.Initial` detects swipe-up (>100px threshold).
- **Divider alpha consistency** — all dividers use `DividerAlpha` (0.4f).
- **Arrow icon size consistency** — AboutRow uses `KraftIconSize.Small` (16dp).
- **ShimmerGrid viewport** — uses `LocalConfiguration.current.screenHeightDp.dp` instead of hardcoded 600dp.
- **About section spacing** — all rows use consistent `Spacing12` vertical padding.

### Removed
- **Dead strings** — `version_tap_hint`, `copied` (no longer used).
- **Old adaptive icon XMLs** — replaced with proper foreground PNGs and `mipmap-anydpi-v26` XML.
- **VectorDrawable splash** — replaced with PNG-based splash icon.

## [1.14.0] - 2026-09-03

### Changed
- **Filter panel redesigned** — flat layout with section dividers, "Filters" title at top, cleaner visual hierarchy.
- **Filter chip colors** — Purity chips now match Wallhaven's native style: SFW (green), Sketchy (amber). Categories, Sorting, Orientation keep the standard teal/blue palette.
- **Reset/Apply buttons** — Reset is now a red outlined button (secondary action), Apply is a filled button (primary action). Clear visual hierarchy.
- **Tab bar icon alignment** — icons shifted down 4dp (top 12 / bottom 4 padding) for better vertical centering with labels.
- **Legacy color aliases** — `AccentPink`, `AccentIndigo` etc. now reference Aurora palette directly instead of allocating new `Color` objects via `get()`.
- **SearchFilterBar spacing** — hardcoded `14.dp` replaced with `KraftSpacing.Spacing16` token; broken indentation fixed.
- **Design token comments** — "iOS" and "Apple HIG" references removed from codebase; neutral design language throughout.

### Fixed
- **Detail screen z-order** — shared element was rendering above chrome (back button, action bar, bottom panel). Fixed using `renderInSharedTransitionScopeOverlay(zIndexInOverlay = 1f)` on all chrome elements within `SharedTransitionScope` context.
- **"More details" hint collision** — panel collapsed height was incorrectly subtracting nav bar inset, causing the hint to overlap the gesture bar. Fixed by stripping only the `Spacing16` measurement overhead from the formula.
- **DetailScreen sharedTransitionScope** — parameter now passed from `DetailScreen` to `DetailContent` so the overlay modifier can be applied correctly.
- **Unused import cleanup** — `zIndex` import removed from DetailContent.kt, `ViewCompat` import removed (no longer needed after height formula fix).

## [1.13.0] - 2026-09-01

### Changed
- **File reorganization** — `Favorite` moved to `domain/model/Favorite.kt`, `WallpaperPosition` and `DownloadedFile` to `domain/model/`, `ImageCache` and `GridImageLoader` to `core/cache/`, `ElapsedClock` to `util/ElapsedClock.kt`. Each type now lives in the package that owns its responsibility.
- **Naming consistency** — `Purity.SfW` renamed to `Purity.SFW`, `Wallpaper.isSfw` to `isSFW` across 6 files. Acronyms now follow standard casing conventions.
- **Shimmer loading** — grid placeholder animation changed from pulse (alpha oscillation) to left-to-right sweep gradient, matching modern skeleton loading patterns.
- **Empty state** — icon circle enlarged to 80dp with 40dp icon, title uses `titleLarge`, proper KDoc added.
- **Detail panel** — drag handle alpha reduced to 0.3, panel background changed to `surfaceContainerLow`, uploader skeleton pulse range tightened to 0.3–0.5.
- **Settings section headers** — typography changed to `labelLarge` for consistency with design tokens.
- **Tab bar colors** — `HigInactiveGray` and `HigSeparator` moved to `KraftColors.TabBarInactive` and `KraftColors.TabBarSeparator` for centralized color management.
- **Filter panel strings** — "Categories", "Purity", "Sorting", "Orientation", "Reset", "Apply" now use string resources for localization support.
- **Rate limit default** — magic number `45` extracted to `KraftConstants.RateLimitDefaultRemaining`.

### Fixed
- **Mojibake in Settings** — bullet character `•` was displaying as garbled `â€¢` in cache description and API key display. Fixed encoding.
- **Scope leak in DetailScreen** — `val setTarget` declaration was at wrong indentation level, leaking outside its intended block.
- **Fully qualified enum references** — `WallpaperRepositoryImpl` now uses imported `Purity.SFW` instead of full path.
- **Fully qualified imports in BrowseScreen** — 8 inline fully qualified references replaced with proper top-level imports.
- **No-op padding** — removed `.padding(horizontal = 0.dp)` from tab bar.
- **All references to "Apple" removed** — 26 comment references across 8 files replaced with neutral design language.

### Removed
- **Dead code** — `GlassPill` composable, `isDownloaded()`, `GlassBlurPx`, `FavoriteDao.getById()` (all previously unused).
- **Haze library** — fully removed (dependencies, imports, params, usage).

## [1.12.2] - 2026-08-31

### Changed
- **Bottom bar synced to 220ms** — `NavigationBar` now uses `AnimatedVisibility` with `slideInVertically(tween(220))+fadeIn` / `slideOutVertically(tween(220))+fadeOut`, so it slides and fades in lockstep with the shared element instead of popping instantly.
- **Detail chrome all 220ms** — every `AnimatedVisibility`, `animateFloatAsState`, `Animatable`, and `Crossfade` in `DetailContent` unified to `tween(220)`: top bar, right-edge pills, panel alpha, panel settle, loading indicators, uploader crossfade, pull hint. No more 200/250/300ms stragglers.
- **Rounded corners morph on detail side** — `ZoomableImage` inner Box now takes `clipRadius = 12dp * (1 - backgroundAlpha)`, so corners morph 12dp→0dp on entry in sync with the background fade. Grid side keeps static clip (shared element masks the return).

### Fixed
- **Shared element return broken** — moving `sharedElementModifier` to the outer clipped `Box` in `WallpaperCard` broke Compose's bounds resolution during pop, causing images to jump to (0,0) before returning to the grid. Moved back to `AsyncImage` where it was working; corner morphing handled on detail side via `clipRadius`.

## [1.12.1] - 2026-08-31

### Added
- **Shared element container-transform transition** — tapping a grid tile now animates the image from its exact grid position to full-screen detail (and back) over 220ms, matching Wallhavener's signature interaction.
- **Smooth black background fill** — the detail background fades from transparent to black in sync with the shared element bounds animation, so the transition feels like one continuous motion instead of a jump + overlay.

### Changed
- **Modifier chain order** — `sharedElement` modifier placed before `graphicsLayer` and layout modifiers per Compose docs, eliminating the coordinate-conflict bug that blocked the transition in v1.12.0.
- **Grid ContentScale** — `WallpaperCard` now uses `ContentScale.Fit` (matching `ZoomableImage`), so shared-element content is identical on both sides of the transition. No visible grid change since tile aspect ratios already match image ratios.
- **Detail enter/exit transitions** — replaced `fadeIn`/`fadeOut` with `EnterTransition.None` + manual `Animatable` background alpha, giving precise control over the background fade timing independent of the navigation transition.

### Fixed
- **Shared element not animating** — root cause was `graphicsLayer` (pinch-zoom scale/translation) placed BEFORE `sharedElement` in `ZoomableImage`, which overrode the bounds animation. Moving `sharedElement` first fixes the tile→full-screen morph.
- **Background snapping to black instantly** — the old `fadeIn(tween(220))` faded the entire composable (including the solid-black background), making it appear instantly. Now the background alpha is animated separately from 0→1 over the same 220ms duration.

## [1.12.0] - 2026-08-22

### Changed
- **Dual query fix** — removed `query` field from `WallpaperListUiState`; all screens now use `filters.query` only, eliminating an entire class of sync bugs.
- **Clock abstraction** — `ElapsedClock` interface injected into `WallpaperListViewModel` so `refresh()` is testable in pure JUnit without Android framework.
- **Fake progress bar replaced** — detail screen now shows an indeterminate shimmer pulse instead of a fake determinate 0→1 animation.
- **Landscape support** — `StaggeredGridCells.Fixed(2)` replaced with `Adaptive(150dp)` in `WallpaperGrid` + `ShimmerGrid`; grid auto-adjusts 2→3→4 columns by width.
- **Crop dialog double-tap** — now matches detail screen: 3-state cycle (fit → fill → native → fit).
- **Compose BOM version inconsistency fixed** — `ui-test-junit4` hardcoded version removed, now uses BOM-managed version.

### Added
- **Haptic feedback on long-press** — `HapticFeedbackType.LongPress` on grid tile long-press (`WallpaperCard`) and download list long-press (`DownloadedList`).
- **`ACCESS_NETWORK_STATE` permission** — enables connectivity checks for better error messages.
- **Predictive back gesture** — `android:enableOnBackInvokedCallback="true"` for Android 13+.
- **`ElapsedClock` abstraction** — injectable time source for testable refresh timing.
- **27 new tests** — `WallpaperListViewModelTest` (10), `BrowseViewModelTest` (6), `ErrorMessagesTest` (11), `WallpaperActionsDataTest` (4), `FavoriteImageStoreTest` (3 fixed). Total: 66 → 94 tests.

### Fixed
- **Moji-bake encoding** — `"â€""` → `"—"` in SettingsScreen (corrupted UTF-8 bytes).
- **Double padding** — FavoritesScreen bottom padding was applied twice.
- **Detail race condition** — `_uiState.value.wallpaper` → `it.wallpaper` inside update lambda.
- **Main-thread deletion** — DownloadsScreen file deletion wrapped in `scope.launch { withContext(Dispatchers.IO) }`.
- **Selection state desync** — FavoritesScreen and DownloadsScreen now derive `selectionMode` from `selectedIds.isNotEmpty()`.
- **Missing permission** — `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"` added.
- **Dead callback removed** — `onZoomChanged = {}` removed from WallKraftNavHost.
- **ProGuard tightened** — narrowed `okhttp3.**` → `okhttp3.internal.**`, added Room entity/DAO keep rules.
- **`security-crypto` upgraded** — from `1.1.0-alpha06` to `1.1.0` stable.

## [1.11.0] - 2026-08-22

### Changed
- **Bottom bar redesigned** — 90dp height, 32dp icon-only tabs (no labels), no capsule indicator, clean minimal look.
- **Settings screen rewritten** — grouped cards (Appearance, Browsing, Data, Advanced, About); all 4 filters (Categories, Purity, Sorting, Orientation) now in Browsing card; cache display + clear; API key dialog with eye toggle; simplified About (version + GitHub only).
- **Filter order unified** — Categories → Purity → Sorting → Orientation in both Settings and the filter sheet.
- **Filter labels unified** — "Categories" / "Purity" / "Sorting" / "Orientation" everywhere via `displayName()`.
- **Filter spacing tightened** — chip gaps 8→6dp, label top 8→6dp, dividers 16→12dp in both Settings and filter sheet.
- **Purity now persisted** — multi-select purity (SFW / Sketchy) saved across sessions; old installs default to SFW.
- **Detail bottom panel gradient** — subtle dark-blue tint (`#0A1420`) added for better readability on dark wallpapers.
- **Empty state centering** — icon + text block now properly centered vertically on all screens (Favorites, Downloads, Browse).

### Added
- **Favorites: multi-select & delete** — long-press any wallpaper to enter selection mode; select all, deselect, batch remove from favorites with confirmation dialog.
- **Downloads: long-press to select** — long-press any download row to enter selection mode directly.
- **WallpaperCard: long-press support** — `onLongClick` parameter with selection check overlay (blue circle + checkmark).
- **WallpaperGrid: selection pass-through** — `onLongClick`, `selectionMode`, `selectedIds`, `onToggleSelect` params for grid-based selection.
- **Unified top bar** — `KraftTopBar` component used across all screens (Browse, Favorites, Downloads, Settings).
- **`KraftTopBar.kt`** — new shared top bar component.

### Fixed
- **Zoom pan bounding** — Fit-aware clamp in ZoomableImage; first double-tap centers fill symmetrically, no black-bar drift.
- **Crop dialog zoom** — identical 3-level cycle (fit → fill → native) as detail screen, centered fill on toggle.
- **Search purity param** — `toPurityParam()` now correctly sends `110`/`100` etc.; repository filters match.
- **Default categories** — `100 → 111` (General + Anime + People); tag/uploader searches force all categories.
- **Settings build** — removed unresolved `LocalClipboard` import; added `Color` + `size` imports to NavHost.
- **EmptyState spacing** — removed internal `Spacing32` padding; horizontal padding on text only.

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
