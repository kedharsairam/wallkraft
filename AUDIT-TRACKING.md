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

**Status:** ⬜ Not started
**Estimated time:** 1-2 hours
**Risk:** Zero

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 1 | Fix moji-bake `"â€""` → `"—"` | `SettingsScreen.kt:99-100` | ⬜ | Shows garbled em dash |
| 2 | Fix double padding | `FavoritesScreen.kt:198` | ⬜ | `padding(innerPadding).padding(bottom = navBarPadding)` |
| 3 | Fix DetailViewModel race condition | `DetailViewModel.kt:85` | ⬜ | `_uiState.value` read inside `_uiState.update` |
| 4 | Fix DownloadsScreen main-thread deletion | `DownloadsScreen.kt:226` | ⬜ | `WallpaperActions.delete()` in onClick |
| 5 | Fix selection state desync | `FavoritesScreen.kt:85-86` | ⬜ | Separate `mutableStateOf` variables |
| 6 | Add `WRITE_EXTERNAL_STORAGE` for API 26-28 | `AndroidManifest.xml` | ⬜ | Crash on Android 8.x-9.x |
| 7 | Remove dead `onZoomChanged` callback | `NavHost.kt:236` | ⬜ | Accepted but discarded |
| 8 | Remove redundant `error("unreachable")` | `WallhavenApi.kt:136` | ⬜ | After infinite loop |
| 9 | Tighten ProGuard `-dontwarn okhttp3.**` | `proguard-rules.pro:2` | ⬜ | → `-dontwarn okhttp3.internal.**` |
| 10 | Add explicit Room entity/DAO keep rules | `proguard-rules.pro` | ⬜ | Don't rely on Room's own rules |
| 11 | Migrate `security-crypto` from alpha to stable | `build.gradle.kts:128` | ⬜ | Alpha dep in production |

---

## Phase 2: Testing Sprint

**Status:** ⬜ Not started
**Estimated time:** 4-6 hours
**Risk:** Low (tests don't change production code, but may surface bugs to fix)

| # | Task | File(s) | Status | Notes |
|---|---|---|---|---|
| 12 | Test `WallpaperActions.kt` — download, set, share, delete, isDownloaded | `WallpaperActions.kt` (338 lines) | ⬜ | Highest-risk gap. Mock Context. |
| 13 | Test `WallpaperListViewModel` — setFilters, refresh, retry, empty results | `WallpaperListViewModel.kt` | ⬜ | Base class for all browsing |
| 14 | Test `ErrorMessages.kt` — error-to-string mapping | `ErrorMessages.kt` | ⬜ | Trivial, quick win |
| 15 | Test `EncryptedApiKeyStore` — fallback logic | `EncryptedApiKeyStore.kt` | ⬜ | Security-critical |
| 16 | Fix `FavoriteImageStoreTest` — 1GB eviction test is a lie | `FavoriteImageStoreTest.kt:68-81` | ⬜ | Never actually tests eviction |
| 17 | Add Compose UI tests — empty states, selection mode, filter sheet | Various `*Screen.kt` | ⬜ | `ui-test-junit4` already in deps |
| 18 | Add navigation test — tab switching, detail→browse back stack | `WallKraftNavHost.kt` | ⬜ | Core flow |

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

---

*Last updated: August 22, 2026*
