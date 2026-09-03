# WallKraft

A clean, fast, private wallpaper app for Android. Powered by [Wallhaven](https://wallhaven.cc).

No ads. No analytics. No trackers. Your data never leaves your device.

## Features

- **Browse** — infinite-scroll masonry grid of wallpapers from Wallhaven
- **Search & Filter** — text search with category, purity, sort, and orientation filters. All filters persist across sessions.
- **Detail View** — full-resolution viewing with pinch-to-zoom, pan, and 3-level double-tap cycle (fit → fill → native)
- **Set as Wallpaper** — crop, position, and apply to home, lock, or both screens
- **Favorites** — save wallpapers locally with long-press multi-select and batch delete. Full-res copies stored for offline viewing.
- **Downloads** — track downloaded files with multi-select and batch delete. Open file location directly.
- **Share** — share the actual image file with any app
- **Data Saver** — defers full-resolution downloads until you zoom
- **Offline-first** — search results and favorites cached for offline use
- **Dark Mode** — follows system theme (light, dark, or system default)

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
│   ├── downloads/        Downloads library + batch delete
│   ├── favorites/        Favorites screen + ViewModel
│   ├── settings/         Settings screen + ViewModel
│   ├── common/           Shared ViewModel logic
│   └── components/       Reusable UI components
└── util/                 Helpers (WallpaperActions, formatting)
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
