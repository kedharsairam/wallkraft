# WallKraft

A clean, fast wallpaper browsing app for Android powered by the [Wallhaven](https://wallhaven.cc) API.

## Features

- **Browse** — infinite-scroll staggered grid of wallpapers from Wallhaven
- **Search & Filter** — text search with category, sort, order, and top-range filters
- **Detail View** — tap any wallpaper for full-resolution view with zoom & pan
- **Set as Wallpaper** — apply to home screen, lock screen, or both
- **Favorites** — save wallpapers locally with Room database
- **Downloads** — track and manage downloaded wallpapers
- **Fullscreen Viewer** — immersive fullscreen with gesture controls
- **Pull to Refresh** — refresh the current feed with a pull gesture
- **Dark Mode** — full dark theme support

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material3
- **Navigation:** Compose Navigation
- **Networking:** Retrofit + OkHttp
- **Image Loading:** Coil
- **Database:** Room
- **Architecture:** MVVM with Repository pattern
- **DI:** Manual dependency injection (AppContainer)

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/java/com/wallkraft/app/
├── core/design/          # Design tokens, theme, colors, typography
├── data/                 # API service, database, repository implementations
├── domain/               # Models, repository interfaces, use cases
├── presentation/         # Screens, ViewModels, components
│   ├── browse/           # Browse screen + ViewModel
│   ├── detail/           # Detail screen + FullscreenViewer
│   ├── downloads/        # Downloads screen
│   ├── favorites/        # Favorites screen + ViewModel
│   └── settings/         # Settings screen + ViewModel
└── util/                 # Helpers (WallpaperActions, extensions)
```

## Testing

```bash
./gradlew test
```

Unit tests cover BrowseViewModel, FavoritesViewModel, SettingsViewModel, DetailViewModel, and FavoriteDao.

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection (for Wallhaven API)

## License

Private — All rights reserved.
