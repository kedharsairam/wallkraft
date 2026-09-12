# WallKraft

A clean, fast, private wallpaper app for Android. Powered by [Wallhaven](https://wallhaven.cc). Built to high quality: every pixel, every transition, every line of code.

No ads. No analytics. No trackers. Your data never leaves your device.

<p align="center">
  <a href="https://github.com/kedharsairam/wallkraft/releases/latest"><img src="https://img.shields.io/badge/Download-APK-blue?style=for-the-badge" alt="Download APK"></a>
  <img src="https://img.shields.io/badge/Quality-High--grade-black?style=for-the-badge" alt="High-grade">
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT">
</p>

## Why high quality?

WallKraft is not just functional — it’s crafted. 157 issues audited across Architecture, Code, Design, A11y, Motion, Security — **P0 18/18 + P1 68/68 fixed** (Hilt, Result/AppError, domain purity, atomic cache, light/dark, sheets). Every interaction is purposeful: 60fps, 44dp touch targets, 8px rhythm, ReduceMotion respected, offline-first, private by default.

## Features

- **Browse** — infinite masonry grid (adaptive columns), pull-to-refresh, skeleton sweep, prefetch + shared-element hero (tile → detail)
- **Search & Filter** — text search with history/suggestions; categories, purity (SFW/Sketchy/NSFW gated by API key), orientation, Wallhaven color pills (29→8 families), sorting `relevance→hot + toplist time range`. Filters persist.
- **Detail** — full-bleed viewer, pinch-to-zoom + 3-step double-tap (fit→fill→native, hard-lock at fill, no black-bar drift), tags, uploader, stats, share
- **Set as Wallpaper** — frame with crop/zoom, apply to Home/Lock/Both, framing remembered for rotation; Showcase/Atmosphere blur modes
- **Favorites** — full-res offline copies (atomic write, LRU 100MB), custom collections (many-to-many, covers, counts), multi-select, batch remove, offline re-validation
- **Rotation** — auto-rotate favorites or one collection (Hourly/Daily/Weekly, boundary-aligned WorkManager chain, battery-not-low), manual Rotate now, showcase blur
- **Share & Downloads** — share files via FileProvider, downloads tracked with badges, public Downloads integration
- **Data Saver** — full-res only on zoom/set/share; offline-first cache (30-min TTL, 100 entries, stale fallback)
- **Design** — OLED true-black `#000000` + light `#F2F2F7`, Aurora palette (Blue/Green/Red/Orange), 8px rhythm, 12/20 radii, Liquid Glass tab bar (AGSL blur, 4-layer frost)

## Screenshots

| Browse | Filters | Detail | Favorites |
|--------|---------|--------|-----------|
| masonry + skeleton | 8 families + shades | full-bleed + zoom | collections + rotation |

> Add screenshots to `docs/screenshots/` — grid, filter panel, detail, favorites.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose + Material 3, Navigation Compose |
| DI | Hilt 2.52 |
| Networking | OkHttp + kotlinx.serialization |
| Images | Coil 3 (disk 512MB, mem 25%, GridImageLoader) |
| Database | Room 2.6 (v4 collections), DataStore |
| Security | EncryptedSharedPreferences AES256-GCM |
| Architecture | MVVM + Repository + UseCases, `Result<AppError>` |
| Testing | JUnit + Espresso + Room Testing (414 unit + 76 instrumented) |

## Build

```bash
cd android
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release (needs key.properties)
./gradlew test               # unit
./gradlew connectedAndroidTest # instrumented (pixel_6)
./gradlew lintDebug          # lint
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk` (19.8MB)

## Project Structure

```
android/app/src/main/java/com/wallkraft/app/
├── core/
│   ├── design/   KraftTheme (Light/Dark), KraftTokens (colors/spacing/radius)
│   ├── errors/   AppError hierarchy
│   ├── cache/    ImageCache/GridImageLoader
│   └── utils/    ReduceMotion, KraftHaptics, Result, validators
├── data/
│   ├── api/      WallhavenApi (Result<AppError>)
│   ├── cache/    SearchResponseCache (atomic), FavoriteImageStore
│   ├── db/       WallKraftDatabase (favorites + collections)
│   ├── mappers/  CollectionMappers
│   ├── prefs/    SettingsStore, RotationStore
│   └── repository/ RepositoryImpl (Result + offline)
├── di/           AppModule (Hilt SingletonComponent)
├── domain/
│   ├── model/    Wallpaper, Collection (pure), WallhavenFilters
│   └── repository/ Interfaces + AppError
├── presentation/
│   ├── browse/   BrowseScreen + ViewModel (Hilt)
│   ├── detail/   DetailScreen/Content/BottomPanel + ZoomableImage
│   ├── favorites/ Favorites + Collections + Strip/Dialogs (ModalBottomSheet)
│   ├── settings/ Settings sections + dialogs
│   ├── common/   WallpaperListViewModel (Result)
│   └── components/ WallpaperGrid/Card, FilterColor, ShimmerGrid, glass/
└── WallKraftApplication (Hilt) + WallKraftNavHost
```

## Quality

* **Quality 90/100** — 157-issue audit, P0/P1 fixed, `lint + assembleDebug + testDebugUnitTest` green, `isSystemInDarkTheme()` light/dark, `ModalBottomSheet Hero 20 + 36x6 pill`, `44dp` hits, `ReduceMotion+Lifecycle` everywhere
* **Privacy:** `PRIVACY.md` — no accounts/analytics/trackers, API key in Keystore, excluded from backup, HTTPS only
* **Offline-first:** search cache, favorites disk, WorkManager chain, no polling

## Requirements

- Android 8.0 (API 26) +
- Internet for Wallhaven API (cached works offline)

## Privacy

Private by default — see [PRIVACY.md](PRIVACY.md). Your API key and favorites never leave the device.

## Support

If you enjoy WallKraft, buy me a coffee:

<p align="center">
  <a href="https://buymeacoffee.com/kedhartech"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="182"></a>
</p>

## License

[MIT](LICENSE)
