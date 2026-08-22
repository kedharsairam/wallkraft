# WallKraft

A clean, fast, and private wallpaper browsing app for Android, powered by the [Wallhaven](https://wallhaven.cc) API.

No ads, no analytics, no trackers — your data stays on your device.

## Features

- **Browse** — infinite-scroll staggered grid of wallpapers from Wallhaven
- **Search & Filter** — text search with category, purity (SFW / Sketchy / Naughty), sort, and orientation filters; all filters persisted across sessions
- **Detail View** — full-resolution view with pinch-to-zoom and pan; 3-level double-tap cycle (fit → fill → native)
- **Set as Wallpaper** — crop and position the image, then apply to home, lock, or both screens
- **Favorites** — save wallpapers locally with **long-press multi-select & batch delete**; full-res copies stored for offline viewing
- **Downloads** — track downloaded files with **long-press multi-select & batch delete**; open file location
- **Share** — share the actual image file with any app
- **Data Saver** — optional mode that defers full-resolution downloads until you zoom
- **Offline-first** — search results and favorites are cached so the app keeps working without a connection
- **Fullscreen Viewer** — immersive viewing with gesture controls
- **Pull to Refresh** — refresh the feed with a pull gesture
- **Dark Mode** — follows your system theme (light, dark, or system)
- **Grouped Settings** — Appearance, Browsing (all 4 filters), Data (cache + saver), Advanced (API key), About
- **Localized** — English and Hindi

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Networking | OkHttp + kotlinx.serialization |
| Image loading | Coil 3 |
| Database | Room (with migrations) |
| Preferences | DataStore + EncryptedSharedPreferences |
| Architecture | MVVM with Repository pattern |
| DI | Manual (AppContainer) |

## Build

```bash
# From the android/ directory
cd android

# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Unit tests
./gradlew test
```

The debug APK is output to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
├── android/               # Android app (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/wallkraft/app/
│       ├── core/design/          # Design tokens, theme, colors, typography
│       ├── data/                 # API, database, cache, preferences, repositories
│       │   ├── api/              # Wallhaven API client
│       │   ├── cache/            # Search response + favorite image caching
│       │   ├── db/               # Room database (favorites)
│       │   ├── prefs/            # DataStore-backed settings
│       │   └── repository/       # Repository implementations
│       ├── domain/               # Models and repository interfaces
│       ├── presentation/         # Screens, ViewModels, shared components
│       │   ├── browse/           # Browse screen + ViewModel
│       │   ├── detail/           # Detail screen + fullscreen viewer
│       │   ├── downloads/        # Downloads library + batch delete
│       │   ├── favorites/        # Favorites screen + ViewModel
│       │   ├── settings/         # Settings screen + ViewModel
│       │   ├── common/           # Shared ViewModel logic
│       │   └── components/       # Reusable UI components
│       └── util/                 # Helpers (WallpaperActions, formatting)
```

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection for the Wallhaven API (cached content works offline)

## Privacy

WallKraft is private by default:

- No accounts, no ads, no analytics, no trackers
- Your Wallhaven API key (optional) is stored only on your device
- Favorites and downloads are stored locally and never leave your device

See [android/docs/privacy-policy.md](android/docs/privacy-policy.md) for details.

## License

[MIT](LICENSE)
