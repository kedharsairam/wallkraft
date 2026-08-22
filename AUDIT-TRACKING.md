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

**Status:** ⬜ Not started
**Estimated time:** 3-4 hours
**Risk:** Low (structural changes, tested on device after each)

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 19 | Split `WallpaperListUiState` — extract sub-states | `WallpaperListViewModel.kt` | ⬜ | 12-field monolith → pagination/filter/error |
| 20 | Extract `WallpaperActions` into injectable service | `WallpaperActions.kt` | ⬜ | Make testable, remove static methods |
| 21 | Fix dual source of truth — `state.query` vs `state.filters.query` | `WallpaperListViewModel.kt:76,111` | ⬜ | Bug waiting to happen |
| 22 | Centralize error handling in base ViewModel | `WallpaperListViewModel.kt` | ⬜ | Consistent error→message mapping |
| 23 | Wire `onZoomChanged` to hide bottom bar or remove | `NavHost.kt`, `DetailScreen.kt` | ⬜ | Currently dead callback |

---

## Phase 4: UX Polish

**Status:** ⬜ Not started
**Estimated time:** 6-8 hours
**Risk:** Low (additive changes, no existing behavior altered)

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 24 | Replace fake progress bar with real/shimmer | `DetailScreen.kt:468-480` | ⬜ | Users feel misled |
| 25 | Add loading skeletons on initial load | `WallpaperGrid.kt`, `FavoritesScreen.kt` | ⬜ | Replace spinner |
| 26 | Add rate-limit UI banner with cooldown timer | `WallKraftNavHost.kt`, new composable | ⬜ | Currently silent failure |
| 27 | Add landscape support — adaptive grid columns | `WallpaperGrid.kt` | ⬜ | 2→3→4 columns by width |
| 28 | Cache settings screen data | `SettingsScreen.kt:104-121` | ⬜ | Don't recompute per navigation |
| 29 | Add haptic feedback — long-press, favorite, download | Various | ⬜ | Feels alive |
| 30 | Fix share action — share image file, not URL | `WallpaperActions.kt` | ⬜ | README says "image file" |
| 31 | Add animation — selection mode, favorite toggle, filters | Various | ⬜ | Modern feel |

---

## Phase 5: Hardening

**Status:** ⬜ Not started
**Estimated time:** 30 minutes
**Risk:** Zero

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 32 | Add `ACCESS_NETWORK_STATE` permission | `AndroidManifest.xml` | ⬜ | Better error messages |
| 33 | Add `android:enableOnBackInvokedCallback` | `AndroidManifest.xml` | ⬜ | Android 13+ predictive back |
| 34 | Fix BOM version inconsistency | `build.gradle.kts:139` | ⬜ | `ui-test-junit4` hardcoded vs BOM |
| 35 | Verify Compose BOM version exists | `build.gradle.kts:101` | ⬜ | `2025.04.00` — may not exist |

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

---

*Last updated: August 22, 2026*
