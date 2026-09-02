# WallKraft — Audit Tracking

**Created:** September 2, 2026
**Standard:** Apple-level quality across every dimension
**Total Issues:** 50

## Status Legend
- `[ ]` — Pending
- `[x]` — Fixed
- `[-]` — Won't fix (documented reason)
- `[~]` — Partially fixed

---

## P0 — Critical (Must Fix)

| # | Issue | File | Status | Commit |
|---|-------|------|--------|--------|
| 1 | Path traversal in FavoriteImageStore | `data/cache/FavoriteImageStore.kt` | [ ] | |
| 2 | Browse tab selected state broken for tag routes | `WallKraftNavHost.kt` | [ ] | |
| 3 | Missing POST_NOTIFICATIONS permission | `AndroidManifest.xml` | [ ] | |
| 4 | ErrorMessages HTTP 429 mapped to wrong message + untested | `util/ErrorMessages.kt`, `util/ErrorMessagesTest.kt` | [ ] | |

## P1 — High (Should Fix)

| # | Issue | File | Status | Commit |
|---|-------|------|--------|--------|
| 5 | FavoriteImageMaxBytes = 1GB (likely bug) | `core/design/KraftTokens.kt` | [ ] | |
| 6 | Silent runCatching in FavoriteImageStore.save() | `data/cache/FavoriteImageStore.kt` | [ ] | |
| 7 | Silent runCatching in SearchResponseCache.put() | `data/cache/SearchResponseCache.kt` | [ ] | |
| 8 | Unused imports (KraftTopBar, DetailScreen, WallpaperActions) | 3 files | [ ] | |
| 9 | EncryptedApiKeyStore claims to log but doesn't | `data/prefs/EncryptedApiKeyStore.kt` | [ ] | |
| 10 | SettingsStore read-then-write race in update() | `data/prefs/SettingsStore.kt` | [ ] | |
| 11 | WallpaperError.RateLimited has null message | `domain/repository/WallpaperRepository.kt` | [ ] | |

## P2 — Medium (Important for Quality)

| # | Issue | File | Status | Commit |
|---|-------|------|--------|--------|
| 12 | Hardcoded strings in SettingsScreen | `presentation/settings/SettingsScreen.kt` | [ ] | |
| 13 | ~30 hardcoded magic numbers in DetailScreen | `presentation/detail/DetailScreen.kt` | [ ] | |
| 14 | Domain layer platform coupling | `WallpaperPosition.kt`, `DownloadedFile.kt` | [ ] | |
| 15 | Inconsistent purity/category representation | `Wallpaper.kt` vs `WallhavenFilters.kt` | [ ] | |
| 16 | No libs.versions.toml version catalog | `build.gradle.kts` | [ ] | |
| 17 | No Gradle caching in CI | `ci.yml`, `release.yml`, `test.yml` | [ ] | |
| 18 | Split DetailScreen.kt (1425 lines) | `presentation/detail/DetailScreen.kt` | [ ] | |
| 19 | SearchResponseCache I/O not on IO dispatcher | `data/cache/SearchResponseCache.kt` | [ ] | |
| 20 | SHA-256 created per call in SearchResponseCache | `data/cache/SearchResponseCache.kt` | [ ] | |
| 21 | FavoriteImageStore.delete() touches LRU | `data/cache/FavoriteImageStore.kt` | [ ] | |
| 22 | Non-WallpaperError exceptions escape repository | `data/repository/WallpaperRepositoryImpl.kt` | [ ] | |
| 23 | Stale meta after filtering | `data/repository/WallpaperRepositoryImpl.kt` | [ ] | |
| 24 | Animation spec duplicated 6+ times | `presentation/detail/DetailScreen.kt` | [ ] | |
| 25 | Redundant CI workflows (ci.yml + test.yml) | `.github/workflows/` | [ ] | |
| 26 | No lint in release workflow | `.github/workflows/release.yml` | [ ] | |

## P3 — Low (Polish)

| # | Issue | File | Status | Commit |
|---|-------|------|--------|--------|
| 27 | Missing KDoc on 15+ files | Multiple | [ ] | |
| 28 | Duplicate CHANGELOG heading | `CHANGELOG.md` | [ ] | |
| 29 | Inconsistent separator alphas | `core/design/KraftTokens.kt` | [ ] | |
| 30 | Spacing14 breaks 8px grid | `core/design/KraftTokens.kt` | [ ] | |
| 31 | Duplicate token values | `core/design/KraftTokens.kt` | [ ] | |
| 32 | Separator default = light mode | `core/design/KraftTokens.kt` | [ ] | |
| 33 | Primary = secondary in theme | `core/design/KraftTheme.kt` | [ ] | |
| 34 | Missing Material3 slots | `core/design/KraftTheme.kt` | [ ] | |
| 35 | Inconsistent fontFamily in typography | `core/design/KraftTheme.kt` | [ ] | |
| 36 | Inconsistent letterSpacing | `core/design/KraftTheme.kt` | [ ] | |
| 37 | Hardcoded container alpha values | `core/design/KraftTheme.kt` | [ ] | |
| 38 | KraftTopBar hardcoded 44.dp and 0.4f | `core/design/KraftTopBar.kt` | [ ] | |
| 39 | Fully-qualified imports in SearchFilterBar | `presentation/components/SearchFilterBar.kt` | [ ] | |
| 40 | Hardcoded 6.dp in FavoritesScreen | `presentation/favorites/FavoritesScreen.kt` | [ ] | |
| 41 | ZoomableImage inconsistent MAX_SCALE | `presentation/components/ZoomableImage.kt` | [ ] | |
| 42 | SettingsScreen inconsistent radius | `presentation/settings/SettingsScreen.kt` | [ ] | |
| 43 | DetailScreen dead onZoomChanged param | `presentation/detail/DetailScreen.kt` | [ ] | |
| 44 | WallpaperActions unused createBitmap import | `util/WallpaperActions.kt` | [ ] | |
| 45 | BrowseScreen scrollToItem jump | `presentation/browse/BrowseScreen.kt` | [ ] | |
| 46 | FavoriteImageStoreTest runBlocking | `data/cache/FavoriteImageStoreTest.kt` | [ ] | |
| 47 | SearchResponseCacheTest Thread.sleep | `data/cache/SearchResponseCacheTest.kt` | [ ] | |
| 48 | Missing KDoc on DisplayNames functions | `util/DisplayNames.kt` | [ ] | |
| 49 | Missing KDoc on WallpaperActions functions | `util/WallpaperActions.kt` | [ ] | |
| 50 | fileSizeFormatted doesn't handle GB+ | `domain/model/Wallpaper.kt` | [ ] | |

---

## Progress

| Priority | Fixed | Total | % |
|----------|-------|-------|---|
| P0 | 0 | 4 | 0% |
| P1 | 0 | 7 | 0% |
| P2 | 0 | 15 | 0% |
| P3 | 0 | 24 | 0% |
| **Total** | **0** | **50** | **0%** |
