# WallKraft

A clean, fast, private wallpaper app for Android. Powered by [Wallhaven](https://wallhaven.cc).

No ads. No analytics. No trackers. Your data never leaves your device.

## Features

- **Browse** — infinite-scroll masonry grid, pull-to-refresh, skeleton loading
- **Search & Filter** — text search with suggestions; categories, purity, orientation, Wallhaven-style color pills, and full Wallhaven sorting (relevance to hot, toplist with time ranges). Filters persist across sessions.
- **Detail View** — full-bleed viewer with pinch-to-zoom and 3-step double-tap (fit → fill → native), tags, uploader info
- **Set as Wallpaper** — frame with crop/zoom, apply to home, lock, or both. Framing is remembered for rotation.
- **Favorites** — full-res offline copies, custom collections, multi-select batch actions
- **Rotation** — auto-rotate favorites or one collection on a schedule, or trigger manually
- **Share & Downloads** — share image files to any app; downloads tracked with on-grid badges
- **Data Saver** — full-resolution only on zoom, set, or share; offline-first caching throughout
- **OLED dark theme** — true-black, wallpaper-first design

## Download

[![Download APK](https://img.shields.io/badge/Download-APK-blue?style=for-the-badge)](https://github.com/kedharsairam/wallkraft/releases/latest)

Or grab the latest from [Releases](https://github.com/kedharsairam/wallkraft/releases).

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Networking | OkHttp + kotlinx.serialization |
| Image Loading | Coil 3 |
| Database | Room |
| Preferences | DataStore + EncryptedSharedPreferences |
| Architecture | MVVM + Repository |
| DI | Manual (AppContainer) |

## Build

```bash
cd android

# Debug
./gradlew assembleDebug

# Release (requires signing config)
./gradlew assembleRelease

# Tests
./gradlew test
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
android/app/src/main/java/com/wallkraft/app/
├── core/design/          Design tokens, theme, colors, typography
├── data/
│   ├── api/              Wallhaven API client
│   ├── cache/            Search response + image caching
│   ├── db/               Room database (favorites)
│   ├── prefs/            DataStore-backed settings
│   └── repository/       Repository implementations
├── domain/               Models and repository interfaces
├── presentation/
│   ├── browse/           Browse screen + ViewModel
│   ├── detail/           Detail screen + fullscreen viewer
│   ├── favorites/        Favorites, collections + ViewModels
│   ├── settings/         Settings screen + section components
│   ├── common/           Shared ViewModel logic
│   └── components/       Reusable UI components
└── util/                 Helpers (download, setter, sharing, formatting)
```

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection for Wallhaven API (cached content works offline)

## Privacy

WallKraft is private by default:

- No accounts, no ads, no analytics, no trackers
- Your Wallhaven API key (optional) is stored only on your device
- Favorites and downloads are stored locally and never leave your device

See [PRIVACY.md](PRIVACY.md) for the full policy.

## Support

If you enjoy using WallKraft, consider buying me a coffee. It keeps the project going.

<p align="center">
  <a href="https://buymeacoffee.com/kedhartech">
    <img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" width="182">
  </a>
</p>

## License

[MIT](LICENSE)
