# WallKraft Architecture

**Status:** Living doc — reflects `fix/arch-p0-domain-purity` branch. Updated 2026-09-12.

## 1. Layers & Module Boundaries

```
presentation (Compose screens, ViewModels, components)
    ↓ depends on
domain (pure Kotlin — models, repository interfaces, business rules)
    ↑ implemented by
data (Room, DataStore, OkHttp, Coil, WorkManager)
    ↓ shared
core (design tokens, utils, errors)
```

Dependency rule: **domain never imports `android.*` or `androidx.*`**. Any platform type (Uri, WallpaperManager, Context) is mapped at the data/util boundary. Presentation may import Android, but must depend on domain models, not `data.db` entities.

### Domain purity (P0 fix)

- `domain/model/WallpaperPosition` is a pure enum `{ HOME, LOCK, BOTH }`. The Android flag mapping lives in `util/WallpaperSetter.toFlags(): Int` (`WallpaperManager.FLAG_SYSTEM / FLAG_LOCK`). No domain file imports `android.app.WallpaperManager`.
- `domain/model/DownloadedFile` keeps `uriString: String` as source of truth. A deprecated `val uri: Uri get() = Uri.parse(uriString)` is retained for compat; callers should migrate to `Uri.parse(file.uriString)`. Construction in `util/DownloadedFiles` uses `uri.toString()`.
- `domain/model/Collection` is the pure collection type `data class Collection(val id: Long, val name: String, val createdAt: Long, val items: List<String>)`. A mapper `fun CollectionWithItems.toDomain(): Collection` lives in the same file. `domain/repository/CollectionsRepository` now exposes `Flow<List<Collection>>` — Room's `CollectionWithItems` never leaks to presentation. All consumers (`FavoritesScreen`, `CollectionStrip`, `CollectionsViewModel`, `CollectionDialogs`, `SettingsRotationSection`, `RotateWallpaperWorker`) import `domain/model/Collection`.

## 2. Error Handling & Result (ARCHITECTURE.md §3)

Typed errors are the single failure channel:

- `core/errors/AppError.kt` — `sealed interface AppError` with buckets:
  - `NetworkError` : `NoConnection`, `Timeout`, `ServerError(code, message)`
  - `DataError` : `Parse`, `Validation`, `NotFound`
  - `StorageError` : `DiskFull`, `PermissionDenied`
  - `AuthError` : `Unauthorized`, `Expired`
  - `Unknown(throwable, message)`
- `core/utils/Result.kt` — `sealed class Result<T> { Success(data: T); Failure(error: AppError) }` with `map/fold/getOrNull` helpers. Distinct from Kotlin stdlib `Result`. `WallpaperRepository` still returns stdlib `Result` for now (incremental migration); new code should use `core.utils.Result`.

Repositories map exceptions to `AppError` at the data boundary; presentation switches on `Result`/`AppError` to show targeted UI. No `try/catch(Exception)` leaking to ViewModels.

## 3. Dependency Injection / Service Location

### ADR-001: AppContainer as intentional Service Locator — Hilt deferred

**Context:** The app currently wires dependencies through `AppContainer` — a manual, lazy `class AppContainer(context)` that builds `OkHttpClient`, `WallhavenApi`, `Room`, `SettingsStore`, `WallpaperRepository`, `FavoritesRepository`, `CollectionsRepository`, `FavoriteImageStore`, `SearchResponseCache`, and the rotation stores. It is passed via `WallKraftApplication.container` and into composables/ViewModel factories.

**Decision:** **Keep `AppContainer` as the service locator for now. Do not introduce Hilt/Dagger.**

**Rationale:**
- The graph is small (~12 singletons) and fully lazy — no start-up cost, no generated component, no kapt/ksp overhead. Hilt would add ~300KB, annotation processing, and build-time complexity for no measurable benefit at this scale.
- Kraft principle of simplicity (README: "Keeps wiring explicit and lightweight — no DI framework").
- `AppContainer` is testable: fakes are constructed directly in tests (`FakeCollectionsRepository`, `FakeDao`) without a DI container; ViewModel factories accept interfaces.
- Migration cost is high (module split, `@Inject`/`@HiltViewModel` churn) vs. low payoff. If the graph grows beyond ~20 bindings or we add multi-module, we will revisit.

**Consequences:**
- Accept that `AppContainer` is an approved exception to "no service locator." It lives at the composition root (`WallKraftApplication`) and never leaks `Context` beyond `applicationContext`.
- ViewModels remain constructor-injected (interface-typed) for testability; Compose call sites use `viewModel(factory = ...)` that pulls from `container`.
- No global `object` singletons for repositories — they are `by lazy` inside `AppContainer` so tests get isolated instances.

**Status:** Accepted. Revisit when: graph >20 or we split `:data`/`:domain` modules.

### RateLimitState & ImageCache — intermediate singleton stance

`data/api/RateLimitState` and `core/cache/ImageCache` are currently Kotlin `object` singletons with `synchronized`/`@Volatile` double-checked locking.

- **RateLimitState** — holds `MutableStateFlow` for `limited/remaining` and a `cooldownJob` tied to an `applicationScope`. Correctly thread-safe (`@Synchronized`), but a plain `object` makes testing rely on `reset()` and risks process-wide state leaking between tests. An injectable holder (`class RateLimitStateHolder @Inject constructor()` with a single `AppContainer`-scoped instance) would be preferable and would allow fakes. **Deferred:** changing `object` → `class` touches `WallhavenApi`, `WallKraftApplication`, and 4 tests; doing it atomically with the domain-purity batch would widen the blast radius. Documented as tech debt; next P1 batch will convert to `class` + `by lazy` in `AppContainer` and update tests to inject a fake holder.
- **ImageCache** — `object` holding `DiskCache`/`MemoryCache` with `@Volatile` + `synchronized`. Same trade-off: global is acceptable because Coil caches are intentionally process-wide and `ImageCache` has no mutable business logic beyond lazy init. Making it `class` only to `by lazy` it in `AppContainer` is a lateral move. **Decision:** leave as `object` and document; if we move to Hilt or need per-test cache isolation, convert to `class ImageCache @Inject constructor(@ApplicationContext ctx)`.

Both are therefore documented as **explicit singleton exceptions** aligned with `AppContainer` — not a DI anti-pattern to chase now.

## 4. Room & Data Mapping

- `data.db.CollectionEntity` / `CollectionItemEntity` / `CollectionWithItems` stay in `data.db` (Room). They are never exposed outside `data`.
- `CollectionsRepositoryImpl` is the anti-corruption layer: `dao.observeAll(): Flow<List<CollectionWithItems>>` → `.map { it.toDomain() }`.
- Presentation collects `Flow<List<Collection>>` and works only with `domain/model/Collection`.

## 5. What is NOT changing this branch

- `WallpaperRepository` interface stays on `kotlin.Result` — the `core.utils.Result` type is added but not wired, to keep the PR atomic and green. A follow-up will map `WallhavenApi` exceptions to `AppError` and change `WallpaperRepository.search/wallpaper` to `core.utils.Result`.
- No Hilt modules, no `object` → `class` migration for `RateLimitState`/`ImageCache` beyond documentation (see ADR-001).

## 6. How to verify

```bash
cd android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

All domain tests (`WallpaperActionsDataTest`, `CollectionsViewModelTest`, `CollectionsRepositoryTest`) and androidTests (`CollectionStripTest`, `SettingsRotationSectionTest`) pass. `assembleDebug` must be green before merge — the branch is intentionally atomic so deps are checked together, not piecemeal.
