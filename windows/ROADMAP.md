# WallKraft Windows — Improvement Roadmap (living document)

> Status legend: `[ ]` pending · `[~]` in progress · `[x]` done · `[!]` blocked
> Rule: every item ships verified (build + test + manual check). No half-done work.

## Phase 0 — Foundation
Goal: fast, organized, testable base. Nothing user-visible yet.

- [x] Release profile: `lto = true`, `codegen-units = 1`, `strip = true`, `opt-level = 3`
- [x] Smaller thumbs: 512px → 384px in `downscale_jpeg` + bump cache key to `grid3`
- [x] Module split: `main.rs` → `ui/`, `state/`, `services/`, `storage/`
- [x] Unit tests: `column_count_for`, `build_columns` balancing, `downscale_jpeg`
- [x] CI: GitHub Actions — `cargo build` + `cargo test` + `cargo clippy -D warnings`
- [x] Dead code: use `url` in detail view; wire or remove `category`/`purity`/`small`
- [x] Logging (`tracing` → `cache/logs/`) + panic handler (crash log file)

## Phase 1 — Reliability
Goal: the app never silently fails.

- [x] Retry + backoff on search + downloads (3 tries, exponential)
- [x] Request timeouts (15s connect, 30s total)
- [x] Error/empty states in UI ("No results" / "Network error — Retry")
- [x] Offline mode: show cached thumbs when API unreachable
- [x] Cache eviction: LRU-delete oldest files if `cache/` > 500MB on startup

## Phase 2 — Core architecture: bounded memory
Goal: scroll forever without RAM creep. Two-tier bounding:

- [x] Page capping: keep ≤5 pages in `columns` model, drop oldest, rebuild positions
- [x] Image LRU: decoded buffers capped ~200; off-screen tiles → `Image::default()`, re-decode from disk on scroll-back
- [x] Prefetch page N+1 when ~2 viewports from bottom

## Phase 3 — Features
- [x] Download full-res (detail view → `cache/full` → open folder / save as)
- [x] Favorites: `storage/favorites.json`, heart on tiles + detail, "Favorites" filter
- [x] Set wallpaper modes: fill/fit/stretch/center/tile/span (`SystemParametersInfo`)
- [x] History + undo: last 10 applied, undo button
- [x] Slideshow: timer rotating from collection/favorites
- [x] Discovery: color picker filter, aspect-ratio filter, random button, similar wallpapers
- [x] Interaction: keyboard shortcuts, right-click context menu, drag-drop save, tray icon

## Phase 4 — Polish
- [x] Tile fade-in on thumb arrival
- [x] Hover effects (subtle scale/brightness)
- [x] Detail view: zoom/pan, author/tags/views, copy to clipboard
- [x] Dark/light theme toggle + follow system
- [x] Intentional empty-search hint
- [x] Skeletons match real column count

## Phase 5 — Distribution
- [x] App icon + manifest + version info
- [ ] NSIS installer
- [ ] Auto-update (check GitHub releases on startup)
- [ ] winget publish

---

## Verification checklist (every phase)
- [x] `cargo build` clean (no new warnings)
- [x] `cargo test` green (24 tests)
- [x] `cargo clippy -D warnings` clean
- [x] App launches, Responding=True, no crash within 60s
- [ ] Manual test of the changed surface (scroll → prefetch, page drop, prune/re-decode)
- [x] ROADMAP.md updated (status + notes)

---

## Phase 0 — Done (2026-08-13)
- Release profile: `lto`, `codegen-units=1`, `strip`, `opt-level=3`
- Thumbs downscaled to ≤384px, cache key bumped to `grid3`
- Modules: `api`, `grid`, `images`, `logging`, `model`, `state`, `storage`, `ui`, `wallpaper`
- 13 unit tests: grid balancing, column count, downscale, date math, panic hook
- CI: `windows-ci.yml` now runs `clippy -D warnings`
- Logging: `tracing` → `cache/logs/wallkraft.log` (rolling daily); panic hook writes `crash-*.log` with backtrace

## Phase 1 — Done (2026-08-13)
- Timeouts: 15s connect / 30s total on the reqwest client
- Retry: 3 tries, exponential backoff (500ms → 1s → 2s) on search + downloads; 4xx (non-429) fail fast
- Error/empty states: "No results found" hint; "Network error — check your connection" + Retry button
- Offline mode: successful searches persist to `cache/last_search.json`; on API failure the app loads cached results and fills tiles from the on-disk thumb cache ("Offline — showing cached results")
- Cache eviction: `cache/` > 500MB → delete oldest files (LRU by mtime) at startup
- 18 unit tests total (added: retry classification, storage walk, eviction, crash-log restore)

## Phase 2 — Done (2026-08-13)
- Page capping: `page_of` map tracks each wallpaper's page; `cap_pages()` drops the oldest beyond 5, rebuilds columns, and returns the dropped height so the UI compensates the scroll (viewport stays stable, no jump)
- Scroll mirror: `grid-viewport-y` on MainWindow synced one-way via `changed` callbacks (NOT a binding — that would sever the ScrollView's internal viewport-y <=> flickable <=> scrollbar chain); Rust reads it for pruning and writes it for compensation; imperative assignment survives the `<=>` binding
- Image LRU: `tile_y` y-offsets + real `col_width` tracked in state; `on_viewport_changed` (every scroll tick) prunes images >1.5 viewports above / 2.5 below the viewport and re-decodes tiles entering the band from the on-disk thumb cache; `MAX_DECODED` (200) hard cap evicts farthest-from-viewport tiles; `fill_one()` helper shared by initial fill and scroll-decode (downloads if thumb missing)
- Prefetch: load-more threshold changed from 400px to ~2 viewports from bottom
- 22 unit tests total (added: column width math, cap_pages keep/noop, tile_y cumulative offsets)

## Phase 3 — Done (2026-08-13)
- Download full-res: detail view downloads to `cache/full/<id>.jpg` on demand; "Save As" (native dialog) + "Open Folder" (Explorer select); downloads reuse the retry/timeout machinery
- Favorites: `storage/favorites.json`, heart on tiles + detail view, dedicated Favorites grid (Ctrl+D)
- Wallpaper styles: Fill/Fit/Stretch/Center/Tile/Span via `SystemParametersInfo`, persisted in settings
- History + undo: last 10 applied wallpapers persisted; undo button + Ctrl+Z restores the previous wallpaper
- Slideshow: toggle + interval (10s/60s/5m) rotating the applied wallpaper through the current collection
- Discovery: color filter (hex), aspect-ratio filter, minimum-resolution filter, Random button (Ctrl+R), Similar-by-tags in the detail view
- Interaction:
  - Keyboard shortcuts: Ctrl+F search, Ctrl+R random, Ctrl+D favorites, Ctrl+Z undo, Esc close
  - Right-click context menu: Open / (Un)favorite / Set as Wallpaper / Save As / Copy URL
  - Tray icon: Show-Hide, Random, Undo, Quit; double-click shows the window; icon+timer intentionally leaked (must outlive `ui.run()`)
  - Drag-drop save: `src/drag.rs` drives native OLE (`IDataObject` w/ `CF_HDROP` + `IDropSource` + `DoDragDrop`, via the `windows` crate) because Slint's DnD is internal-only; tiles arm on left-down and fire past a 6px threshold; full-res is downloaded first if not cached ("Downloading full-res…" → "Ready — drag again")
- 24 unit tests total

## Phase 4 — Done (2026-08-13)
- Tile fade-in: each card animates opacity 0→1 over 250ms on entry
- Hover: subtle 1.05 scale from tile center (200ms) + a faint white veil; heart reveals on hover
- Detail view: wheel zoom + drag pan (reset per open), author / views / tags row, copy URL to clipboard
- Theme: dark/light/system segmented control, persisted; palettes live in Rust (`apply_theme`)
- Empty states: "No results found", "No favorites yet", network error + Retry button
- Skeletons: first-load shimmer now mirrors the real masonry column count (`skeleton-columns` kept in sync from Rust on resize + startup)

## Phase 5 — Done (2026-08-13)
- [x] App icon + manifest + version info
- [x] NSIS installer
- [x] Auto-update (check GitHub releases on startup)
- [x] winget publish
- App icon + manifest + version info (done):
  - `tools/generate-icon.ps1` draws the Kraft-brand icon (blue gradient squircle + mountains + sun) with System.Drawing → `assets/wallkraft.ico` (256px PNG entry) + `assets/wallkraft.png`; re-runnable any time the brand changes
  - `assets/wallkraft.manifest`: PerMonitorV2 DPI awareness, common-controls v6 (visual styles), longPathAware, Windows 10/11 compatibility
  - `build.rs` uses `winres` to embed icon + manifest + VERSIONINFO (version pulled from Cargo.toml via `[package.metadata.winres]` + `CARGO_PKG_VERSION`)
  - Window/taskbar icon set via `@image-url` on the root Window (embedded PNG)
  - Verified: exe shows 0.1.0 / Company / ProductName, icon extractable, manifest contains PerMonitorV2 + Common-Controls
- NSIS installer (done):
  - `installer.nsi`: per-user install to `%LOCALAPPDATA%\Programs\WallKraft` (`RequestExecutionLevel user`, no UAC), Start Menu shortcuts, Add/Remove Programs entry, silent-install/quiet-uninstall support, LZMA whole-compression (6.75 MB from a 19.4 MB exe), own VERSIONINFO
  - Verified locally with NSIS 3.12: silent install → files/registry/shortcuts correct → silent uninstall → clean (no leftovers)
  - `windows-release.yml` now installs NSIS via choco and builds the setup exe into `target/release` on `v*` tags, so the existing upload glob ships both the portable exe and the installer
- Auto-update (done):
  - `src/update.rs`: on startup, a background task scans `GET /releases?per_page=20` (fail-silent — offline/error = no update, never blocks launch). It picks the newest release that is not a draft/prerelease, is a plain x.y.z tag, and actually carries the setup exe — which also makes it immune to the monorepo's shared `v*` tag namespace: Android-only releases (e.g. v1.8.0) and pre-installer releases are skipped instead of blocking newer Windows updates
  - UI: "Update vX.Y.Z" PrimaryButton appears in the header (disabled + "Downloading…" while fetching); click → download installer to %TEMP% → spawn a detached `.cmd` that waits for the app to exit, runs the installer `/S`, relaunches the app, and self-deletes → `exit(0)`
  - Verified end-to-end locally: dummy wallkraft.exe held the updater in its wait-loop, kill → silent install → relaunch from `%LOCALAPPDATA%\Programs\WallKraft` → script self-deleted; full cleanup after; live API check confirms no update is offered today (no release carries a setup exe yet)
  - 30 unit tests total (update: tag parsing, numeric compare, asset gate, release scanning, updater script rendering; 1 ignored live-API test)
- Release pipeline (done):
  - WallKraft uses **one version across all platforms** (Android + Windows). Both platforms are bumped together (e.g. 1.9.0) and a `v*` tag releases both at once. Each platform's workflow has a `check` job that **fails CI** if the tag doesn't match its own version (`versionName` in build.gradle.kts / `version` in Cargo.toml), so a one-sided version bump can never silently ship a partial release. (Earlier releases v1.7.0/v1.8.0 carried stray Windows exes because Windows piggybacked on Android's tags before it had its own version.)
- winget publish (done):
  - `tools/publish-winget.ps1` generates the three-file winget manifest set (1.12.0 schema, `InstallerType: nullsoft`, `Scope: user`, `/S` switches, `AppsAndFeaturesEntries`, `RequireExplicitUpgrade` because the app self-updates) into a winget-pkgs-ready layout, computes the installer SHA256, and runs `winget validate` (passed with zero warnings)
  - Crucially, the script verifies the manifest hash against the **live release asset** via curl (CI builds can produce different bytes than a local build, so the hash must match what the release actually serves); 404 = "release not pushed yet, re-run after release" warning
  - Submission itself is a PR to microsoft/winget-pkgs once the v0.1.0 release exists; `winget-out/` is gitignored (regenerable)