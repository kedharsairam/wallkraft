# WallKraft — Performance, Caching, Polish & Reliability Roadmap

> Tracking document for the current development phase (post v1.5.0).
> Status legend: `[ ]` not started · `[~]` in progress · `[x]` done

**Scope decisions (from Kedhar):**
- No second wallpaper source — Wallhaven stays the single source (filters are the reason it was chosen).
- No analytics of any kind — not even crash reporting. App must be fully privacy-first; no data leaves the device.
- Daily wallpaper / auto-rotate / better discovery are **parked** (not in this phase).
- App is free forever, no monetization.

---

## Phase 0 — Bug fix (do first)

- [x] **Fix duplicate `categories` query param** in `WallhavenApi.kt` `search()` — the parameter is added twice (copy-paste). Fixed; build passes.

## Phase 1 — Quick wins (small effort, high value)

- [x] **Cache tuning** — size Coil's memory + disk cache properly (currently defaults) so the grid is instant without wasting storage. New shared `ImageCache.kt` (512 MB disk, 25% memory) used by both the singleton and grid loaders.
- [x] **Prefetch detail image on tap** — warm the full-res image into the cache before the detail screen opens (added to the grid tap handler, covers Browse/Favorites/Tag).
- [x] **Haptics** — subtle vibration feedback on favorite toggle, download, and successful wallpaper set (detail screen).
- [x] **Dependency updates — HOLD** (user decision 2026-08-07): toolchain is Kotlin 2.1.0 / AGP 8.9.0; the big libraries (Compose, Room, Coil) can't be safely upgraded without a full toolchain bump. Deferred. Revisit when ready for a proper upgrade.

## Phase 2 — Premium caching (bigger, high payoff)

- [x] **API response caching** — new `SearchResponseCache` (file-backed, 30-min TTL, offline fallback, bounded 100 entries). Returning to a visited Browse screen is instant and works offline. Wired into `WallpaperRepositoryImpl` + `AppContainer`.
- [x] **Bug fix: pull-to-refresh stuck indicator** — the cache made `refresh()` return instantly (cache hit), so `isRefreshing` toggled `true→false` within one frame and Material3's `PullToRefreshBox` got stuck showing the spinner. Fixed two ways: (1) `search()` gained a `forceRefresh` param that bypasses the cache (and the offline fallback) so pull-to-refresh returns live data; (2) `refresh()` enforces a `MIN_REFRESH_MS` (500ms) so the indicator always has time to animate away. Verified on-device: refresh now hits the network and takes ~1s.
- [x] **Offline favorites** — new `FavoriteImageStore` (app-private, non-evictable). Full-res downloaded on favorite, deleted on unfavorite; detail screen prefers the local file. Favorites viewable with no internet.

## Phase 3 — Reliability

- [x] **Network retry with backoff** — `WallhavenApi.execute` now retries transient failures (network errors + 5xx) up to 3 times with exponential backoff (1s/2s/4s). Never retries rate-limit (429), client errors, or parse failures. Also rethrows `CancellationException` properly.
- [x] **Startup time audit** — no changes needed. `AppContainer` uses lazy deps, the splash is held only until the theme resolves, and no heavy work blocks the first frame. (Attempted to hoist the ImageLoader factory setup into `remember`, but it's a `@Composable` call and must stay in the composition body — reverted.)

## Phase 4 — Polish pass

- [x] **Accessibility audit** — all icons have content descriptions (decorative ones `null`), touch targets are 48dp. Fixed the hardcoded `"Downloaded"` string in `WallpaperCard.kt` to use the `favorites_downloaded` resource.
- [x] **Edge-to-edge polish** — already complete. `enableEdgeToEdge()`, system-bar icon color matched to theme, per-screen `statusBarsPadding()`, `NavigationBar` handles nav-bar insets, and Detail hides system bars when zoomed. No changes needed.
- [x] **Localization** — added `values-hi/strings.xml` (Hindi) with all 84 strings translated, proving the i18n architecture. Format specifiers (`%1$s`, `%1$d`) preserved.

## Phase 5 — Ongoing / profiling

- [x] **Memory leak audit** — clean by inspection. ViewModels use `viewModelScope` (auto-cancelled on clear), `AppContainer` holds only `applicationContext` (no Activity leak) with all-lazy deps, `WallpaperActions` is a stateless object, and every composable coroutine is scoped to composition (`LaunchedEffect`/`rememberCoroutineScope`). No `GlobalScope` or static context. (A device-side profiler session is still worth doing after the low-end smoothness pass.)
- [ ] **Low-end device smoothness** — verify scrolling on a cheap phone, not just a Pixel. Needs a low-end device + profiler; only the Pixel 8a is available and it wasn't connected this session. Debug APK builds cleanly. Defer to a session with the hardware attached.

---

## Already done (no action needed)

- R8 minification + resource shrinking (APK size) — enabled in `build.gradle.kts`
- Splash screen — `core-splashscreen` + `Theme.WallKraft.Splash`
- Backup — `allowBackup` + `backup_rules` + `data_extraction_rules`
- Empty states — `EmptyState.kt`
- Rate-limit handling — `RateLimitState` + banner
- Network timeouts — 15s connect/read in `AppContainer`
- Grid overdraw — mostly solved by tile cleanup in v1.5.0

---

## Feature batch (post-roadmap, 2026-08-08)

- [x] **Share** — new share button on the detail action stack. Shares the actual image (offline favorite copy, else downloaded to cache) via a new `FileProvider` (`file_paths.xml` + manifest provider), falling back to the wallhaven.cc URL if the image can't be fetched. `WallpaperActions.share()`.
- [x] **Favorites folders** — added then **removed** (2026-08-09). Room schema v1→v2 (`MIGRATION_1_2` adds nullable `collection` column), then v2→v3 (`MIGRATION_2_3` drops it by table recreate for API 26+ compat). Final state: no folders; migrations kept so devices on v2 upgrade cleanly.
- [x] **Set wallpaper with position/crop** — replaced the plain set flow with a full-screen crop dialog (`WallpaperCropDialog`): pinch-zoom + drag to frame the visible region (clamped so the image always covers the screen), position chips (home/lock/both), then crops to screen resolution and applies via `WallpaperManager.setBitmap`. Verified on-device: dialog renders and set succeeds with no crash.
- [x] **Data saver mode** — opt-in Settings toggle (default OFF = current behavior). When ON, the detail screen shows the thumbnail instantly but defers the full-res download until the user zooms (the moment they actually need detail); local files (offline favorites) still load immediately since they cost zero data. A small "Loading full resolution…" pill appears while zoomed before the full-res arrives so the blurry thumbnail never reads as broken. The grid's tap-to-prefetch full-res is gated too (Browse/Favorites/Tag) — opening a wallpaper no longer downloads it. Set/share already download on demand via `imageFile`/`shareableFile`. New `dataSaverMode` field in `AppSettings` + DataStore key; 3 new unit tests (29 total). Verified on-device: opening a wallpaper with data saver ON adds nothing to Coil's cache; zooming downloads the full-res and shows the pill until it lands.

---

## Notes / decisions log

- v1.5.0 released 2026-08-07. This roadmap starts after that release.
- No analytics, no crash reporting, no tracking — by explicit user decision.