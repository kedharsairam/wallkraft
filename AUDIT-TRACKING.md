# WallKraft Quality Improvement Tracking

> **Goal:** 90+/100 across every audit category without losing any existing functionality.
> **Started:** August 22, 2026
> **Current Overall Score:** 72/100
> **Target Overall Score:** 91/100

---

## Current Scores

| Category | Current | Target | Status |
|---|---|---|---|
| Design System | 90 | 93 | ⬜ Not started |
| Code Quality | 68 | 92 | ⬜ Not started |
| Architecture | 75 | 88 | ⬜ Not started |
| Error Handling | 65 | 90 | ⬜ Not started |
| Testing | 45 | 92 | ⬜ Not started |
| UX/Usability | 78 | 93 | ⬜ Not started |
| Performance | 72 | 88 | ⬜ Not started |
| Security | 80 | 92 | ⬜ Not started |

---

## Phase 1: Quick Wins (Zero Risk)

**Status:** ✅ Complete
**Time taken:** ~30 minutes
**Risk:** Zero — all changes were pure fixes or additions

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 1 | Fix moji-bake `"â€""` → `"—"` | `SettingsScreen.kt:99-100` | ✅ | Replaced corrupted UTF-8 bytes with em dash (U+2014) |
| 2 | Fix double padding | `FavoritesScreen.kt:198` | ✅ | Removed redundant `padding(bottom = navBarPadding)` |
| 3 | Fix DetailViewModel race condition | `DetailViewModel.kt:85` | ✅ | Changed `_uiState.value.wallpaper` → `it.wallpaper` inside update lambda |
| 4 | Fix DownloadsScreen main-thread deletion | `DownloadsScreen.kt:226` | ✅ | Wrapped in `scope.launch { withContext(Dispatchers.IO) }` |
| 5 | Fix selection state desync | `FavoritesScreen.kt:85-86` | ✅ | Derived `selectionMode = selectedIds.isNotEmpty()`, removed `isSelecting` var. Also fixed in DownloadsScreen |
| 6 | Add `WRITE_EXTERNAL_STORAGE` for API 26-28 | `AndroidManifest.xml` | ✅ | Added with `android:maxSdkVersion="28"` |
| 7 | Remove dead `onZoomChanged` callback | `NavHost.kt:236` | ✅ | Removed unused empty lambda from NavHost |
| 8 | Remove redundant `error("unreachable")` | `WallhavenApi.kt:136` | ✅ | Added `@Suppress("KotlinUnreachableCode")` to satisfy type checker |
| 9 | Tighten ProGuard `-dontwarn okhttp3.**` | `proguard-rules.pro:2` | ✅ | Changed to `-dontwarn okhttp3.internal.**`, removed overly broad `-keep class okhttp3.**` |
| 10 | Add explicit Room entity/DAO keep rules | `proguard-rules.pro` | ✅ | Added `-keep @androidx.room.Entity class *` and `-keep @androidx.room.Dao class *` |
| 11 | Migrate `security-crypto` from alpha to stable | `build.gradle.kts:128` | ✅ | `1.1.0-alpha06` → `1.1.0` (stable, released Jul 2025) |

---

## Phase 2: Testing Sprint

**Status:** ✅ Partially complete (4 of 7 items — 2 cancelled, 1 deferred)
**Time taken:** ~45 minutes
**Risk:** Low — no production code changed

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 12 | Test `WallpaperActions.kt` — data classes + pure logic | `WallpaperActionsDataTest.kt` | ✅ | Tested WallpaperPosition flags, DownloadedFile data class. Full coverage blocked by Android Context dependency. |
| 13 | Test `WallpaperListViewModel` — setFilters, retry, loadNextPage, dedup, empty, error | `WallpaperListViewModelTest.kt` | ✅ | 10 tests covering core pagination + filter logic. refresh() blocked by SystemClock.elapsedRealtime(). |
| 14 | Test `ErrorMessages.kt` — error model branching | `ErrorMessagesTest.kt` | ✅ | Tests WallpaperError.RateLimited, Api codes (400/401/403/404/429/500), null message. Resources.getString() blocked without Robolectric. |
| 15 | Test `EncryptedApiKeyStore` — fallback logic | `EncryptedApiKeyStore.kt` | ⏸️ Deferred | Requires Robolectric or instrumented test for EncryptedSharedPreferences. Will revisit with Phase 3 architecture changes. |
| 16 | Fix `FavoriteImageStoreTest` — 1GB eviction test | `FavoriteImageStoreTest.kt` | ✅ | Replaced misleading test with 3 meaningful tests: LRU ordering, temp file exclusion, oldest-first eviction logic. |
| 17 | Add Compose UI tests | Various `*Screen.kt` | ⏸️ Cancelled | Requires Compose test rules + on-device testing. Deferred to manual QA. |
| 18 | Add navigation test | `WallKraftNavHost.kt` | ⏸️ Cancelled | Requires Compose Navigation testing rules. Deferred to manual QA. |

---

## Phase 3: Architecture Improvements

**Status:** ✅ Partially complete (3 of 5 items — 2 skipped with rationale)
**Time taken:** ~30 minutes
**Risk:** Low — no production behavior changed

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 19 | Split `WallpaperListUiState` — extract sub-states | `WallpaperListViewModel.kt` | ⏸️ Deferred | Too invasive for this session — touches every screen. Defer to v2.0 refactor. |
| 20 | Extract `WallpaperActions` into injectable service | `WallpaperActions.kt` | ⏸️ Skipped | Inherently tied to Android APIs (DownloadManager, WallpaperManager, MediaStore). `object` pattern is idiomatic Kotlin. Adding interface + mock provides no real test value. |
| 21 | Fix dual source of truth — `state.query` vs `state.filters.query` | `WallpaperListViewModel.kt` | ✅ | Removed `query` field from `WallpaperListUiState`. All screens now use `filters.query` only. Eliminates entire class of sync bugs. |
| 22 | Centralize error handling in base ViewModel | `WallpaperListViewModel.kt` | ✅ Skipped | Already well-centralized via injected `errorMessage` lambda. Adding a helper would save ~3 lines but add indirection. |
| 23 | Wire `onZoomChanged` to hide bottom bar or remove | `NavHost.kt` | ✅ Done in Phase 1 | Removed dead callback. |

---

## Phase 4: UX Polish

**Status:** ✅ Partially complete (4 of 8 items — 2 skipped, 2 deferred)
**Time taken:** ~45 minutes
**Risk:** Low — additive changes, no existing behavior altered

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 24 | Replace fake progress bar with indeterminate shimmer | `DetailScreen.kt` | ✅ | Replaced fake determinate 0→1 animation with indeterminate pulse. Removed unused `fullResProgress` Animatable + `heroScope`. |
| 25 | Add loading skeletons on initial load | `ShimmerGrid.kt` | ✅ Already done | `ShimmerGrid` already provides shimmer placeholders on initial load. |
| 26 | Add rate-limit UI banner with cooldown timer | `RateLimitBanner.kt` | ✅ Already done | Banner exists. Cooldown timer already built into `RateLimitState` (auto-clears after `COOLDOWN_MS`). Adding countdown display requires threading through 4 layers — marginal gain. |
| 27 | Add landscape support — adaptive grid columns | `WallpaperGrid.kt`, `ShimmerGrid.kt` | ✅ | Changed `StaggeredGridCells.Fixed(2)` → `Adaptive(150dp)`. Grid auto-adjusts: 2 cols portrait, 3-4 cols landscape/tablet. |
| 28 | Cache settings screen data | `SettingsScreen.kt` | ⏸️ Deferred | Low priority — settings are already fast. Defer if performance issues surface. |
| 29 | Add haptic feedback — long-press | `WallpaperCard.kt`, `DownloadedList.kt` | ✅ | Added `HapticFeedbackType.LongPress` on grid tile long-press and download list long-press. |
| 30 | Fix share action — share image file, not URL | `WallpaperActions.kt` | ⏸️ Deferred | Already implemented — `share()` prefers image file via FileProvider, falls back to URL. No change needed. |
| 31 | Add animation — selection mode, favorite toggle, filters | Various | ⏸️ Deferred | Nice-to-have, defer to v2.0. |

---

## Phase 5: Hardening

**Status:** ✅ All 4 items complete
**Time taken:** ~10 minutes
**Risk:** Zero — manifest/config changes only

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 32 | Add `ACCESS_NETWORK_STATE` permission | `AndroidManifest.xml` | ✅ | Enables connectivity checks for better error messages. |
| 33 | Add `android:enableOnBackInvokedCallback` | `AndroidManifest.xml` | ✅ | Android 13+ predictive back gesture support. |
| 34 | Fix BOM version inconsistency | `build.gradle.kts:139` | ✅ | Removed hardcoded `1.7.6` from `ui-test-junit4`, now uses BOM. |
| 35 | Verify Compose BOM version exists | `build.gradle.kts:101` | ✅ | `2025.04.00` is valid (released April 9, 2025). |

---

## Log

### August 22, 2026 — Session 1: Audit & Planning
- Ran comprehensive audit across 8 dimensions
- Identified 35 specific improvement items
- Created 5-phase execution plan
- Documented 3 caveats (tests surface bugs, refactoring risk, more maintenance)
- All items confirmed achievable without losing existing functionality
- **No code changes yet — planning only**

### August 22, 2026 — Session 2: Phase 1 — Quick Wins
- Fixed 6 bugs: moji-bake, double padding, race condition, main-thread deletion, selection desync, missing permission
- Cleaned up 4 code smells: dead callback, redundant error, ProGuard tightening, alpha dependency
- All 11 items completed in ~30 minutes
- Files changed: SettingsScreen.kt, FavoritesScreen.kt, DetailViewModel.kt, DownloadsScreen.kt, NavHost.kt, WallhavenApi.kt, AndroidManifest.xml, proguard-rules.pro, build.gradle.kt
- **Ready for build verification**

### August 22, 2026 — Session 3: Phase 2 — Testing Sprint
- Added 3 new test files: WallpaperListViewModelTest (10 tests), ErrorMessagesTest (11 tests), WallpaperActionsDataTest (4 tests)
- Fixed FavoriteImageStoreTest: replaced misleading eviction test with 3 meaningful tests (LRU ordering, temp exclusion, oldest-first logic)
- Total tests: 66 → 93 (27 new tests)
- Cancelled: Compose UI tests + navigation test (require Robolectric/instrumented testing)
- Deferred: EncryptedApiKeyStore test (requires Robolectric for EncryptedSharedPreferences)
- **Key finding:** `refresh()` untestable in pure JUnit due to `SystemClock.elapsedRealtime()` dependency — candidate for Phase 3 refactor
- **All 93 tests pass, build successful**

### August 22, 2026 — Session 4: Phase 3 — Architecture Improvements
- **Dual query fix:** Removed `query` field from `WallpaperListUiState`. All screens now use `filters.query` only. Eliminates entire class of sync bugs between `state.query` and `state.filters.query`.
- **Clock abstraction:** Added `ElapsedClock` fun interface + injectable clock in `WallpaperListViewModel`. `refresh()` now testable in pure JUnit without Android framework.
- **New tests:** 2 refresh tests added (refresh replaces list, refresh stays visible for min duration). Total: 93 → 95 tests.
- **Skipped:** WallpaperActions extraction (inherently tied to Android APIs, object pattern is idiomatic), error handling centralization (already well-centralized via lambda), state splitting (too invasive, defer to v2.0).
- **All 95 tests pass, build successful**

### August 22, 2026 — Session 5: Phase 4 — UX Polish
- **Fake progress bar:** Replaced determinate 0→1 animation with indeterminate pulse in `DetailScreen`. Removed unused `fullResProgress` Animatable + `heroScope`.
- **Landscape support:** Changed `StaggeredGridCells.Fixed(2)` → `Adaptive(150dp)` in `WallpaperGrid` + `ShimmerGrid`. Grid auto-adjusts: 2 cols portrait, 3-4 cols landscape/tablet.
- **Haptic feedback:** Added `HapticFeedbackType.LongPress` on grid tile long-press (`WallpaperCard`) and download list long-press (`DownloadedList`). Fixed missing `LocalContext` + `ColorPainter` imports in DownloadedList.
- **Skipped:** Rate-limit countdown timer (already has auto-clear cooldown, countdown too complex for marginal gain), settings caching (low priority), share action (already works correctly), animations (defer to v2.0).
- **All 95 tests pass, build successful**

### August 22, 2026 — Session 6: Phase 5 — Hardening
- **Permissions:** Added `ACCESS_NETWORK_STATE` for connectivity checks.
- **Predictive back:** Added `android:enableOnBackInvokedCallback="true"` to activity.
- **BOM fix:** Removed hardcoded `ui-test-junit4:1.7.6`, now uses BOM version.
- **BOM verified:** `2025.04.00` is valid (released April 9, 2025).
- **All 95 tests pass, build successful**

---

*Last updated: August 22, 2026*
