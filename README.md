# WallKraft

A clean, fast, and private wallpaper browsing app for Android and Windows, powered by the [Wallhaven](https://wallhaven.cc) API.

No ads, no analytics, no trackers — your data stays on your device.

This is a monorepo. Each platform lives in its own directory with its own toolchain:

| Directory | Platform | Stack |
|---|---|---|
| `android/` | Android | Kotlin + Jetpack Compose |
| `windows/` | Windows | Rust + Slint |

## Windows app

Native Rust + Slint desktop app — browse, set, and manage Wallhaven wallpapers:

- **Staggered masonry grid** with infinite scroll, first-load skeletons, and tile fade-ins
- **Search & filters** — text search, category / purity / sort / orientation / color / resolution / aspect ratio
- **Set as wallpaper** — apply to the desktop with undo support
- **Drag & drop** — drag any tile to the desktop or a folder to save the full-res image
- **System tray** — show/hide, random, undo, quit
- **Detail view** — zoom, pan, copy URL, find similar wallpapers
- **Favorites, slideshow, offline-first caching, dark / light / system theme**
- **Installer & auto-update** — per-user NSIS installer (no admin), automatic in-app updates
- Portable single-exe build available alongside the installer

## Features

- **Browse** — infinite-scroll staggered grid of wallpapers from Wallhaven
- **Search & Filter** — text search with category, sort, and orientation filters
- **Detail View** — full-resolution view with pinch-to-zoom and pan
- **Set as Wallpaper** — crop and position the image, then apply to home, lock, or both screens
- **Favorites** — save wallpapers locally; full-res copies are stored for offline viewing
- **Downloads** — track downloaded files, open their location, and delete them — including **batch select & delete**
- **Share** — share the actual image file with any app
- **Data Saver** — optional mode that defers full-resolution downloads until you zoom
- **Offline-first** — search results and favorites are cached so the app keeps working without a connection
- **Fullscreen Viewer** — immersive viewing with gesture controls
- **Pull to Refresh** — refresh the feed with a pull gesture
- **Dark Mode** — follows your system theme (light, dark, or system)
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

## Build — Windows

```bash
# From the windows/ directory
cd windows

# Debug build
cargo build

# Release build
cargo build --release

# Unit tests + lint
cargo test
cargo clippy -- -D warnings

# NSIS installer (requires NSIS 3.x on PATH or at the default install path)
makensis /DVERSION=1.9.0 /DEXE_PATH="target\release\wallkraft.exe" installer.nsi
```

The release exe is `windows/target/release/wallkraft.exe`; the installer is `WallKraft-<version>-setup.exe`.

> **Versioning:** WallKraft uses one version number across all platforms. Android and Windows are bumped together (currently 1.9.0), and a `v*` tag releases both at once. See [CHANGELOG.md](CHANGELOG.md).

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
└── windows/               # Windows app (Rust + Slint)
    ├── src/                      # App modules (api, state, storage, tray, update, …)
    ├── ui/main.slint             # Slint UI
    ├── installer.nsi             # NSIS installer script
    ├── tools/                    # generate-icon.ps1, publish-winget.ps1
    └── ROADMAP.md                # Windows roadmap (all phases complete)
```

## Requirements

- Android 8.0 (API 26) or higher
- Windows 10 / 11 (x64)
- Internet connection for the Wallhaven API (cached content works offline)

## Privacy

WallKraft is private by default:

- No accounts, no ads, no analytics, no trackers
- Your Wallhaven API key (optional) is stored only on your device
- Favorites and downloads are stored locally and never leave your device

See [android/docs/privacy-policy.md](android/docs/privacy-policy.md) for details.

## License

[MIT](LICENSE)