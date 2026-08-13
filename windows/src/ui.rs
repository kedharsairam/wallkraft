//! UI actions: search, detail view, wallpaper application, status toasts.
//! All functions here are invoked from Slint callbacks and run their async
//! work on the tokio runtime, marshalling results back to the UI thread via
//! `upgrade_in_event_loop`.

use std::collections::HashSet;
use std::sync::{Arc, OnceLock};

use slint::{ComponentHandle, Image, Model, ModelRc, SharedString, VecModel, Weak};
use tokio::sync::Semaphore;

use crate::api;
use crate::grid::{apply_columns, tile_ratio};
use crate::images::downscale_jpeg;
use crate::state::{
    cache, cap_pages, SharedState, View, KEEP_ABOVE_VIEWPORTS, KEEP_BELOW_VIEWPORTS, MAX_DECODED,
};
use crate::storage;
use crate::wallpaper;
use crate::{KraftTheme, MainWindow, WallpaperData};

/// Re-run the current search (page 1) after a filter change, guarded by busy.
pub fn reload(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    let query = {
        let mut guard = state.lock().unwrap();
        if guard.busy {
            return;
        }
        guard.busy = true;
        guard.page = 1;
        guard.query.clone()
    };
    start_search(ui, client, handle, state, query, 1, false);
}

/// Fetch a page of search results and stream tiles into the grid.
///
/// Wallhaven's `thumbs.large`/`small` are hard 16:9 crops, so tiles are built
/// from `thumbs.original` (true aspect ratio) downscaled to <=384px on a
/// background thread — mirrors what Android does (Coil loads original, scales
/// to fit). The masonry layout is shown immediately from API metadata (each
/// tile keeps its real ratio) and thumbs fill in progressively as they arrive,
/// so the UI thread never stalls on decode.
pub fn start_search(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    query: String,
    page: u32,
    append: bool,
) {
    ui.set_loading(true);
    let ui_weak: Weak<MainWindow> = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let cache = cache();
    handle.clone().spawn(async move {
        let filters = {
            let guard = state.lock().unwrap();
            guard.filters.clone()
        };
        match client.search(&query, page, &filters).await {
            Ok(mut wallpapers) => {
                // Persist the latest results so a later offline launch can
                // still show cached content. Best-effort — never fail the
                // search over a disk hiccup.
                if !append {
                    let _ = storage::save_last_search(&cache, &wallpapers);
                }
                // Record the page each wallpaper came from, then drop the
                // oldest pages beyond MAX_PAGES. Returns the height of the
                // dropped content so the UI can compensate the scroll.
                let (dropped_height, all) = {
                    let mut guard = state.lock().unwrap();
                    if append {
                        for w in &wallpapers {
                            guard.page_of.insert(w.id.clone(), page);
                        }
                        guard.wallpapers.append(&mut wallpapers);
                    } else {
                        guard.page_of.clear();
                        for w in &wallpapers {
                            guard.page_of.insert(w.id.clone(), page);
                        }
                        guard.wallpapers = wallpapers;
                    }
                    guard.page = page;
                    guard.busy = false;
                    let dropped_height = cap_pages(&mut guard);
                    (dropped_height, guard.wallpapers.clone())
                };
                // Assign columns from metadata only — no image work, so the
                // grid (with correct tile ratios) appears instantly. The
                // model of models and the position map are built on the UI
                // thread because WallpaperData holds slint::Image.
                let all_ui = all.clone();
                let state_ui = state.clone();
                let ui_weak_ok = ui_weak.clone();
                let _ = ui_weak_ok.upgrade_in_event_loop(move |ui| {
                    apply_columns(&ui, &state_ui, &all_ui);
                    // Compensate the scroll for dropped pages so the view
                    // doesn't jump (viewport-y is negative while scrolled).
                    if dropped_height > 0.0 {
                        let y = ui.get_grid_viewport_y() + dropped_height;
                        ui.set_grid_viewport_y(y.min(0.0));
                    }
                    ui.set_loading(false);
                    ui.set_status(SharedString::default());
                    // Empty result set → show a friendly hint instead of a
                    // blank grid. Only for fresh searches, not "load more".
                    if all_ui.is_empty() && !append {
                        ui.set_empty_message("No results found".into());
                        ui.set_error(false);
                    } else {
                        ui.set_empty_message(SharedString::default());
                    }
                });

                fill_thumbs(&all, &client, &handle, &state, &ui_weak, &cache);
            }
            Err(e) => {
                state.lock().unwrap().busy = false;
                tracing::error!("search failed: {e:#}");
                let msg = format!("Search failed: {e}");
                // Offline fallback: if the API is unreachable but we have
                // cached results from a previous session, show them instead
                // of a blank error screen. Thumbs load from disk.
                let cached = storage::load_last_search(&cache).ok();
                let ui_weak_err = ui_weak.clone();
                let ui_weak_inner = ui_weak_err.clone();
                let _ = ui_weak_err.upgrade_in_event_loop(move |ui| {
                    ui.set_loading(false);
                    if let Some(cached) = cached
                        && !cached.is_empty()
                    {
                        let state_ui = state.clone();
                        apply_columns(&ui, &state_ui, &cached);
                        ui.set_empty_message(SharedString::default());
                        ui.set_error(false);
                        flash_status(&ui, "Offline — showing cached results");
                        // Populate tiles from the on-disk thumb cache.
                        fill_thumbs(&cached, &client, &handle, &state, &ui_weak_inner, &cache);
                        return;
                    }
                    // Only surface the full-screen error state when there's
                    // nothing to show; otherwise keep the grid and toast.
                    if ui.get_columns().row_count() == 0 {
                        ui.set_empty_message("Network error — check your connection".into());
                        ui.set_error(true);
                    }
                    flash_status(&ui, msg);
                });
            }
        }
    });
}

/// Shared cap on concurrent thumb downloads/decodes (network + disk), across
/// both the initial fill and the scroll-triggered re-decode.
static THUMB_SEM: OnceLock<Arc<Semaphore>> = OnceLock::new();
fn thumb_sem() -> Arc<Semaphore> {
    THUMB_SEM.get_or_init(|| Arc::new(Semaphore::new(8))).clone()
}

/// Download (if needed), downscale, decode and set one tile's image. Runs on a
/// background task; the model write is marshalled back to the UI thread. The
/// (col,row) is read from state at completion time (not captured at spawn) so
/// a resize that re-balanced the grid mid-download still lands in the right
/// cell. Already-cached thumbs (offline mode) just decode from disk — no
/// network.
fn fill_one(
    ui_weak: &Weak<MainWindow>,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    cache: &std::path::Path,
    w: crate::model::Wallpaper,
) {
    let sem = thumb_sem();
    let ui_weak = ui_weak.clone();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let cache = cache.to_path_buf();
    handle.spawn(async move {
        let _permit = sem.acquire_owned().await;
        let id = w.id.clone();
        let dest = storage::thumb_path(&cache, &id);

        // Make sure a downscaled thumb exists on disk.
        if !dest.exists() {
            let url = if !w.thumbs.original.is_empty() {
                w.thumbs.original.clone()
            } else {
                w.thumbs.large.clone()
            };
            if client.download_to(&url, &dest).await.is_err() {
                return;
            }
            let d2 = dest.clone();
            if tokio::task::spawn_blocking(move || downscale_jpeg(&d2))
                .await
                .ok()
                .flatten()
                .is_none()
            {
                // Keep the full-res file as a fallback.
            }
        }

        // Decode to a raw pixel buffer off the UI thread
        // (slint::Image is not Send, but SharedPixelBuffer is).
        let buf = {
            let d = dest.clone();
            tokio::task::spawn_blocking(move || {
                let img = image::open(&d).ok()?;
                let rgb = img.to_rgb8();
                let (w, h) = (rgb.width(), rgb.height());
                Some(slint::SharedPixelBuffer::<slint::Rgb8Pixel>::clone_from_slice(
                    rgb.as_raw(), w, h,
                ))
            })
            .await
            .ok()
            .flatten()
        };
        let Some(buf) = buf else { return };

        // Poke the tile's image in on the UI thread.
        let _ = ui_weak.upgrade_in_event_loop(move |ui| {
            let pos = {
                let guard = state.lock().unwrap();
                guard.positions.get(&id).copied()
            };
            let Some((col, row)) = pos else { return };
            let outer = ui.get_columns();
            let Some(ovm) = outer
                .as_any()
                .downcast_ref::<VecModel<ModelRc<WallpaperData>>>()
            else {
                return;
            };
            let Some(inner) = ovm.row_data(col) else { return };
            if let Some(vm) = inner.as_any().downcast_ref::<VecModel<WallpaperData>>()
                && let Some(mut d) = vm.row_data(row)
                && d.id.as_str() == id
            {
                d.thumb = Image::from_rgb8(buf);
                vm.set_row_data(row, d);
            }
            state.lock().unwrap().decoded.insert(id);
        });
    });
}

/// Download + downscale each thumb on background threads and fill its tile in
/// as it becomes ready. Bounded concurrency keeps us polite to wallhaven.cc
/// and RAM predictable. Only tiles near the viewport are filled eagerly; the
/// rest decode lazily on scroll (on_viewport_changed), so the initial burst is
/// proportional to what's visible, not the whole capped page set.
fn fill_thumbs(
    wallpapers: &[crate::model::Wallpaper],
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    ui_weak: &Weak<MainWindow>,
    cache: &std::path::Path,
) {
    let (tile_y, col_width, scroll_y, visible_height) = {
        let guard = state.lock().unwrap();
        (guard.tile_y.clone(), guard.col_width, guard.scroll_y, guard.visible_height)
    };
    // Before the first layout we have no positions/width — fill everything.
    let fill_all = visible_height <= 0.0 || col_width <= 0.0 || tile_y.is_empty();
    let keep_top = scroll_y - KEEP_ABOVE_VIEWPORTS * visible_height;
    let keep_bottom = scroll_y + (1.0 + KEEP_BELOW_VIEWPORTS) * visible_height;
    for w in wallpapers {
        let in_band = fill_all
            || match tile_y.get(&w.id) {
                Some(&top) => {
                    let bottom = top + col_width * tile_ratio(w);
                    bottom >= keep_top && top <= keep_bottom
                }
                // Position unknown (freshly appended page) → fill.
                None => true,
            };
        if in_band {
            fill_one(ui_weak, client, handle, state, cache, w.clone());
        }
    }
}

/// Called on every scroll tick (UI thread). Tracks the scroll position and
/// prunes/decodes tile images so only tiles near the viewport hold a decoded
/// buffer — the second half of bounded memory (page capping bounds the model,
/// this bounds decoded pixels). Pruned tiles re-decode from the on-disk thumb
/// cache when scrolled back into view.
pub fn on_viewport_changed(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    scroll_y: f32,
    visible_height: f32,
) {
    let (tile_y, col_width, wallpapers, decoded) = {
        let mut guard = state.lock().unwrap();
        guard.scroll_y = scroll_y;
        guard.visible_height = visible_height;
        (
            guard.tile_y.clone(),
            guard.col_width,
            guard.wallpapers.clone(),
            guard.decoded.clone(),
        )
    };
    if visible_height <= 0.0 || col_width <= 0.0 {
        return;
    }
    let keep_top = scroll_y - KEEP_ABOVE_VIEWPORTS * visible_height;
    let keep_bottom = scroll_y + (1.0 + KEEP_BELOW_VIEWPORTS) * visible_height;

    let mut to_prune: HashSet<String> = HashSet::new();
    let mut to_decode: Vec<crate::model::Wallpaper> = Vec::new();
    for w in &wallpapers {
        let Some(&top) = tile_y.get(&w.id) else { continue };
        let bottom = top + col_width * tile_ratio(w);
        if decoded.contains(&w.id) {
            // Has an image: prune it once it leaves the keep band.
            if bottom < keep_top || top > keep_bottom {
                to_prune.insert(w.id.clone());
            }
        } else if bottom >= keep_top && top <= keep_bottom {
            // No image yet but near the viewport: decode it.
            to_decode.push(w.clone());
        }
    }

    // Hard cap: if decoded images still exceed MAX_DECODED (possible if the
    // page cap is relaxed later), evict the tiles farthest from the viewport
    // center until we're back under it.
    if decoded.len() > MAX_DECODED {
        let center = scroll_y + visible_height / 2.0;
        let mut far: Vec<(f32, String)> = decoded
            .iter()
            .filter_map(|id| {
                let top = *tile_y.get(id)?;
                let w = wallpapers.iter().find(|w| &w.id == id)?;
                let bottom = top + col_width * tile_ratio(w);
                let dist = if bottom < center {
                    center - bottom
                } else if top > center {
                    top - center
                } else {
                    0.0
                };
                Some((dist, id.clone()))
            })
            .collect();
        far.sort_by(|a, b| b.0.total_cmp(&a.0)); // farthest first
        let excess = decoded.len() - MAX_DECODED;
        for (_, id) in far.into_iter().take(excess) {
            to_prune.insert(id);
        }
    }

    // Prune: clear images for tiles far off-screen.
    if !to_prune.is_empty() {
        let outer = ui.get_columns();
        if let Some(ovm) = outer.as_any().downcast_ref::<VecModel<ModelRc<WallpaperData>>>() {
            for ci in 0..ovm.row_count() {
                if let Some(inner) = ovm.row_data(ci)
                    && let Some(vm) = inner.as_any().downcast_ref::<VecModel<WallpaperData>>()
                {
                    for ri in 0..vm.row_count() {
                        if let Some(mut d) = vm.row_data(ri)
                            && to_prune.contains(d.id.as_str())
                        {
                            d.thumb = Image::default();
                            vm.set_row_data(ri, d);
                        }
                    }
                }
            }
        }
        let mut guard = state.lock().unwrap();
        for id in &to_prune {
            guard.decoded.remove(id);
        }
    }

    // Decode: fill tiles entering the keep band (from disk, or network if the
    // thumb was never downloaded).
    let cache = cache();
    for w in to_decode {
        fill_one(&ui.as_weak(), client, handle, state, &cache, w);
    }
}

/// Show the detail view for a wallpaper. Renders the cached thumbnail
/// instantly, then swaps in the full-resolution image once it downloads.
pub fn open_detail(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    id: String,
) {
    let ui_weak: Weak<MainWindow> = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let cache = cache();
    handle.spawn(async move {
        let Some((w, thumb_dest, full_dest, is_fav)) = ({
            let guard = state.lock().unwrap();
            guard
                .wallpapers
                .iter()
                .find(|w| w.id == id)
                .cloned()
                .map(|w| {
                    let thumb_dest = storage::thumb_path(&cache, &w.id);
                    let full_dest = storage::full_path(&cache, &w.id);
                    let is_fav = guard.favorites.contains(&w.id);
                    (w, thumb_dest, full_dest, is_fav)
                })
        }) else {
            return;
        };

        // Instant preview from the cached thumbnail (no download).
        let resolution = w.resolution.clone();
        let wid = w.id.clone();
        let wurl = w.url.clone();
        let author = w.user.as_ref().map(|u| u.username.clone()).unwrap_or_default();
        let views = w.views;
        let tags: Vec<String> = w.tags.iter().take(6).map(|t| t.name.clone()).collect();
        let _ = ui_weak.upgrade_in_event_loop(move |ui| {
            let image = Image::load_from_path(&thumb_dest).unwrap_or_default();
            ui.set_detail_image(image);
            ui.set_detail_resolution(resolution.into());
            ui.set_detail_id(wid.into());
            ui.set_detail_url(wurl.into());
            ui.set_detail_author(author.into());
            ui.set_detail_views(format!("{views} views").into());
            ui.set_detail_tags(tags.join(" · ").into());
            ui.set_detail_favorite(is_fav);
            ui.invoke_reset_detail_view();
            ui.set_detail_visible(true);
        });

        // Download the full-resolution original and swap it in when ready.
        if full_dest.exists() || client.download_to(&w.path, &full_dest).await.is_ok() {
            let _ = ui_weak.upgrade_in_event_loop(move |ui| {
                ui.set_detail_image(Image::load_from_path(&full_dest).unwrap_or_default());
            });
        }
    });
}

/// Show a transient status toast that clears itself after a few seconds.
pub fn flash_status(ui: &MainWindow, msg: impl Into<SharedString>) {
    ui.set_status(msg.into());
    let weak = ui.as_weak();
    // slint::Timer is Copy — the runtime keeps it alive until it fires, so
    // dropping the handle here is fine.
    slint::Timer::single_shot(std::time::Duration::from_secs(3), move || {
        if let Some(ui) = weak.upgrade() {
            ui.set_status(SharedString::default());
        }
    });
}

/// Download the full-res image (if needed) and apply it as the desktop wallpaper.
pub fn apply_wallpaper(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    id: String,
) {
    let ui_weak: Weak<MainWindow> = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let cache = cache();
    handle.spawn(async move {
        let (wallpaper, style) = {
            let guard = state.lock().unwrap();
            let w = guard.wallpapers.iter().find(|w| w.id == id).cloned();
            (w, wallpaper::WallpaperStyle::from_index(guard.wallpaper_style as usize))
        };
        let Some(w) = wallpaper else { return };
        let dest = storage::full_path(&cache, &w.id);
        if !dest.exists() && client.download_to(&w.path, &dest).await.is_err() {
            tracing::error!("download failed for {}", w.id);
            let msg = format!("Download failed for {}", w.id);
            let _ = ui_weak.upgrade_in_event_loop(move |ui| flash_status(&ui, msg));
            return;
        }
        let msg = match wallpaper::set_wallpaper(&dest, style) {
            Ok(()) => {
                tracing::info!("wallpaper applied: {}", w.id);
                // Record in history (newest first, capped at 10) for Undo.
                let path = dest.to_string_lossy().to_string();
                let mut guard = state.lock().unwrap();
                guard.history.retain(|p| p != &path);
                guard.history.insert(0, path);
                guard.history.truncate(10);
                let _ = storage::save_history(&cache, &guard.history);
                "Wallpaper set".to_string()
            }
            Err(e) => {
                tracing::error!("set_wallpaper failed for {}: {e:#}", w.id);
                format!("Failed to set wallpaper: {e}")
            }
        };
        let _ = ui_weak.upgrade_in_event_loop(move |ui| flash_status(&ui, msg));
    });
}

// ---------------------------------------------------------------------------
// Settings, favorites, slideshow, and detail-view actions.
// ---------------------------------------------------------------------------

const ATLEAST_OPTIONS: [&str; 4] = ["", "1920x1080", "2560x1440", "3840x2160"];
const RATIO_OPTIONS: [&str; 9] = [
    "", "16x9", "9x16", "4x3", "3x4", "16x10", "10x16", "21x9", "32x9",
];
const THEME_OPTIONS: [&str; 3] = ["dark", "light", "system"];
const SLIDESHOW_INTERVALS: [u64; 4] = [10, 30, 60, 300];

/// Persist the user-tunable settings to disk.
fn persist_settings(state: &SharedState) {
    let guard = state.lock().unwrap();
    let _ = storage::save_settings(
        &cache(),
        &storage::Settings {
            theme: guard.theme.clone(),
            wallpaper_style: guard.wallpaper_style,
            slideshow_interval_secs: guard.slideshow_interval_secs,
        },
    );
}

fn hex_color(hex: &str) -> slint::Color {
    let hex = hex.trim_start_matches('#');
    let r = u8::from_str_radix(&hex[0..2], 16).unwrap_or(0);
    let g = u8::from_str_radix(&hex[2..4], 16).unwrap_or(0);
    let b = u8::from_str_radix(&hex[4..6], 16).unwrap_or(0);
    slint::Color::from_rgb_u8(r, g, b)
}

/// Whether Windows is in dark mode (AppsUseLightTheme = 0). Defaults to dark.
fn system_dark_mode() -> bool {
    use windows::core::PCWSTR;
    use windows::Win32::System::Registry::{
        RegGetValueW, HKEY_CURRENT_USER, RRF_RT_REG_DWORD,
    };
    let path = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    let name = "AppsUseLightTheme";
    let path_wide: Vec<u16> = path.encode_utf16().chain(std::iter::once(0)).collect();
    let name_wide: Vec<u16> = name.encode_utf16().chain(std::iter::once(0)).collect();
    let mut value: u32 = 0;
    let mut size = std::mem::size_of::<u32>() as u32;
    let result = unsafe {
        RegGetValueW(
            HKEY_CURRENT_USER,
            PCWSTR::from_raw(path_wide.as_ptr()),
            PCWSTR::from_raw(name_wide.as_ptr()),
            RRF_RT_REG_DWORD,
            None,
            Some(&mut value as *mut u32 as *mut core::ffi::c_void),
            Some(&mut size),
        )
    };
    result.is_ok() && value == 0
}

/// Apply the Kraft theme palette to the UI.
pub fn apply_theme(ui: &MainWindow, theme: &str) {
    let dark = match theme {
        "light" => false,
        "system" => system_dark_mode(),
        _ => true,
    };
    let (bg, surface, surface2, surface3, text1, text2, text3, separator) = if dark {
        (
            "#000000", "#1C1C1E", "#2C2C2E", "#3A3A3C", "#FFFFFF", "#EBEBF5", "#8E8E93",
            "#38383A",
        )
    } else {
        (
            "#F2F2F7", "#FFFFFF", "#E5E5EA", "#D1D1D6", "#000000", "#3C3C43", "#8E8E93",
            "#D1D1D6",
        )
    };
    let t = ui.global::<KraftTheme>();
    t.set_dark(dark);
    t.set_bg(hex_color(bg));
    t.set_surface(hex_color(surface));
    t.set_surface_secondary(hex_color(surface2));
    t.set_surface_tertiary(hex_color(surface3));
    t.set_text_primary(hex_color(text1));
    t.set_text_secondary(hex_color(text2));
    t.set_text_tertiary(hex_color(text3));
    t.set_separator(hex_color(separator));
}

/// Theme segmented control changed.
pub fn theme_changed(ui: &MainWindow, state: &SharedState, index: i32) {
    let theme = THEME_OPTIONS[index as usize].to_string();
    {
        let mut guard = state.lock().unwrap();
        guard.theme = theme.clone();
    }
    persist_settings(state);
    ui.set_theme_index(index);
    apply_theme(ui, &theme);
}

/// Wallpaper style (fill/fit/stretch/tile) changed.
pub fn style_changed(ui: &MainWindow, state: &SharedState, index: i32) {
    {
        let mut guard = state.lock().unwrap();
        guard.wallpaper_style = index as u32;
    }
    persist_settings(state);
    ui.set_style_index(index);
}

/// Slideshow interval changed — restart the timer if a slideshow is running.
pub fn slideshow_interval_changed(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    index: i32,
) {
    let secs = SLIDESHOW_INTERVALS[index as usize];
    {
        let mut guard = state.lock().unwrap();
        guard.slideshow_interval_secs = secs;
    }
    persist_settings(state);
    ui.set_slideshow_interval_index(index);
    if ui.get_slideshow_active() {
        restart_slideshow(ui, client, handle, state);
    }
}

/// Toggle the slideshow on/off.
pub fn slideshow_toggle(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    if ui.get_slideshow_active() {
        stop_slideshow();
        state.lock().unwrap().slideshow_active = false;
        ui.set_slideshow_active(false);
        flash_status(ui, "Slideshow stopped");
    } else {
        state.lock().unwrap().slideshow_active = true;
        ui.set_slideshow_active(true);
        start_slideshow(ui, client, handle, state);
        flash_status(ui, "Slideshow started");
    }
}

/// Whether the slideshow task should keep running. Read by the interval task
/// on the tokio runtime; set by the toggle/restart helpers.
static SLIDESHOW_RUNNING: std::sync::atomic::AtomicBool =
    std::sync::atomic::AtomicBool::new(false);
/// Bumped on every start/stop so a stale task from a previous toggle exits
/// instead of racing a newly spawned one.
static SLIDESHOW_GENERATION: std::sync::atomic::AtomicU64 =
    std::sync::atomic::AtomicU64::new(0);

fn start_slideshow(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    use std::sync::atomic::Ordering;
    use tokio::time::{interval, MissedTickBehavior};
    let ui_weak = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let secs = state.lock().unwrap().slideshow_interval_secs;
    let generation = SLIDESHOW_GENERATION.fetch_add(1, Ordering::Relaxed) + 1;
    SLIDESHOW_RUNNING.store(true, Ordering::Relaxed);
    handle.clone().spawn(async move {
        let mut ticker = interval(std::time::Duration::from_secs(secs));
        ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);
        ticker.tick().await; // first tick fires immediately — skip it
        while SLIDESHOW_RUNNING.load(Ordering::Relaxed)
            && SLIDESHOW_GENERATION.load(Ordering::Relaxed) == generation
        {
            ticker.tick().await;
            if !SLIDESHOW_RUNNING.load(Ordering::Relaxed)
                || SLIDESHOW_GENERATION.load(Ordering::Relaxed) != generation
            {
                break;
            }
            if let Some(ui) = ui_weak.upgrade() {
                slideshow_step(&ui, &client, &handle, &state);
            }
        }
    });
}

fn restart_slideshow(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    stop_slideshow();
    start_slideshow(ui, client, handle, state);
}

fn stop_slideshow() {
    use std::sync::atomic::Ordering;
    SLIDESHOW_RUNNING.store(false, Ordering::Relaxed);
    SLIDESHOW_GENERATION.fetch_add(1, Ordering::Relaxed);
}

/// Advance the slideshow by one wallpaper (cycling through the current pool).
fn slideshow_step(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    let id = {
        let mut guard = state.lock().unwrap();
        let is_fav = guard.view == View::Favorites;
        let len = if is_fav {
            guard.favorite_wallpapers.len()
        } else {
            guard.wallpapers.len()
        };
        if len == 0 {
            return;
        }
        let idx = guard.slideshow_index % len;
        guard.slideshow_index += 1;
        if is_fav {
            guard.favorite_wallpapers[idx].id.clone()
        } else {
            guard.wallpapers[idx].id.clone()
        }
    };
    apply_wallpaper(ui, client, handle, state, id);
}

/// Restore the previously applied wallpaper.
pub fn undo(ui: &MainWindow, state: &SharedState) {
    let (prev, style) = {
        let mut guard = state.lock().unwrap();
        if guard.history.len() < 2 {
            return;
        }
        let prev = guard.history[1].clone();
        guard.history.remove(0);
        (prev, wallpaper::WallpaperStyle::from_index(guard.wallpaper_style as usize))
    };
    let path = std::path::PathBuf::from(&prev);
    let msg = match wallpaper::set_wallpaper(&path, style) {
        Ok(()) => {
            tracing::info!("undo: restored {prev}");
            "Undo: previous wallpaper restored".to_string()
        }
        Err(e) => {
            tracing::error!("undo failed: {e:#}");
            format!("Undo failed: {e}")
        }
    };
    let history = state.lock().unwrap().history.clone();
    let _ = storage::save_history(&cache(), &history);
    ui.set_undo_visible(history.len() >= 2);
    flash_status(ui, msg);
}

/// Toggle a wallpaper in/out of favorites. Updates the tile, the detail view,
/// and (if visible) the favorites grid.
pub fn toggle_favorite(ui: &MainWindow, state: &SharedState, id: &str) {
    let (was_fav, in_favorites_view) = {
        let mut guard = state.lock().unwrap();
        let was_fav = guard.favorites.contains(id);
        if was_fav {
            guard.favorites.remove(id);
            guard.favorite_wallpapers.retain(|w| w.id != id);
        } else {
            guard.favorites.insert(id.to_string());
            if let Some(w) = guard.wallpapers.iter().find(|w| w.id == id).cloned() {
                guard.favorite_wallpapers.retain(|x| x.id != id);
                guard.favorite_wallpapers.insert(0, w);
            }
        }
        let _ = storage::save_favorites(&cache(), &guard.favorite_wallpapers);
        (was_fav, guard.view == View::Favorites)
    };
    set_tile_favorite(ui, id, !was_fav);
    if ui.get_detail_visible() && ui.get_detail_id().as_str() == id {
        ui.set_detail_favorite(!was_fav);
    }
    if in_favorites_view {
        let wallpapers = state.lock().unwrap().favorite_wallpapers.clone();
        apply_columns(ui, state, &wallpapers);
        if wallpapers.is_empty() {
            ui.set_empty_message("No favorites yet — tap the heart on any wallpaper".into());
            ui.set_error(false);
        }
    }
}

/// Update the heart state on the matching tile without rebuilding the grid.
fn set_tile_favorite(ui: &MainWindow, id: &str, fav: bool) {
    let outer = ui.get_columns();
    if let Some(ovm) = outer.as_any().downcast_ref::<VecModel<ModelRc<WallpaperData>>>() {
        for ci in 0..ovm.row_count() {
            if let Some(inner) = ovm.row_data(ci)
                && let Some(vm) = inner.as_any().downcast_ref::<VecModel<WallpaperData>>()
            {
                for ri in 0..vm.row_count() {
                    if let Some(mut d) = vm.row_data(ri)
                        && d.id.as_str() == id
                    {
                        d.favorite = fav;
                        vm.set_row_data(ri, d);
                        return;
                    }
                }
            }
        }
    }
}

/// Toggle between the search grid and the favorites grid.
pub fn show_favorites(ui: &MainWindow, state: &SharedState) {
    let (wallpapers, view) = {
        let mut guard = state.lock().unwrap();
        guard.view = if guard.view == View::Grid { View::Favorites } else { View::Grid };
        let view = guard.view;
        let wallpapers = if view == View::Favorites {
            guard.favorite_wallpapers.clone()
        } else {
            guard.wallpapers.clone()
        };
        (wallpapers, view)
    };
    ui.set_favorites_view(view == View::Favorites);
    ui.set_grid_viewport_y(0.0);
    apply_columns(ui, state, &wallpapers);
    if wallpapers.is_empty() {
        let msg = if view == View::Favorites {
            "No favorites yet — tap the heart on any wallpaper"
        } else {
            "No results found"
        };
        ui.set_empty_message(msg.into());
        ui.set_error(false);
    } else {
        ui.set_empty_message(SharedString::default());
    }
}

/// Random search — sets the sorting to "random" and reloads.
pub fn random_search(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    {
        let mut guard = state.lock().unwrap();
        guard.filters.sorting = "random".into();
    }
    ui.set_sorting_index(3);
    reload(ui, client, handle, state);
}

/// Color filter changed — validate the hex and reload.
pub fn color_changed(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    text: &str,
) {
    let hex = text.trim().trim_start_matches('#').to_lowercase();
    if !hex.is_empty() && (hex.len() != 6 || !hex.chars().all(|c| c.is_ascii_hexdigit())) {
        flash_status(ui, "Color must be a hex value like #aabbcc");
        return;
    }
    {
        let mut guard = state.lock().unwrap();
        guard.filters.color = hex;
    }
    reload(ui, client, handle, state);
}

/// Minimum resolution filter changed.
pub fn atleast_changed(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    index: i32,
) {
    {
        let mut guard = state.lock().unwrap();
        guard.filters.atleast = ATLEAST_OPTIONS[index as usize].into();
    }
    ui.set_atleast_index(index);
    reload(ui, client, handle, state);
}

/// Aspect-ratio filter changed.
pub fn ratio_changed(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    index: i32,
) {
    {
        let mut guard = state.lock().unwrap();
        guard.filters.ratios = RATIO_OPTIONS[index as usize].into();
    }
    ui.set_ratio_index(index);
    reload(ui, client, handle, state);
}

/// Download the full-res image (if needed) and show a native save dialog.
pub fn save_as(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    id: String,
) {
    let ui_weak: Weak<MainWindow> = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let cache = cache();
    handle.spawn(async move {
        let (wid, w, full_dest) = {
            let guard = state.lock().unwrap();
            let Some(w) = guard.wallpapers.iter().find(|w| w.id == id).cloned() else {
                return;
            };
            let full_dest = storage::full_path(&cache, &w.id);
            (w.id.clone(), w, full_dest)
        };
        if !full_dest.exists() && client.download_to(&w.path, &full_dest).await.is_err() {
            let _ = ui_weak.upgrade_in_event_loop(move |ui| flash_status(&ui, "Download failed"));
            return;
        }
        let _ = ui_weak.upgrade_in_event_loop(move |ui| {
            let mut dialog = rfd::FileDialog::new();
            dialog = dialog.set_file_name(format!("{wid}.jpg"));
            if let Some(dest) = dialog.save_file() {
                if std::fs::copy(&full_dest, &dest).is_ok() {
                    flash_status(&ui, format!("Saved to {}", dest.display()));
                } else {
                    flash_status(&ui, "Save failed");
                }
            }
        });
    });
}

/// Reveal the cached full-res file in Explorer.
pub fn open_folder(ui: &MainWindow, state: &SharedState, id: &str) {
    let path = {
        let guard = state.lock().unwrap();
        let Some(w) = guard.wallpapers.iter().find(|w| w.id == id) else {
            return;
        };
        storage::full_path(&cache(), &w.id)
    };
    if path.exists() {
        let _ = std::process::Command::new("explorer")
            .arg(format!("/select,{}", path.display()))
            .spawn();
    } else {
        flash_status(ui, "Not downloaded yet — open it first");
    }
}

/// Search for similar wallpapers using the top tags of the given one.
pub fn similar(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    id: &str,
) {
    let query = {
        let guard = state.lock().unwrap();
        let Some(w) = guard.wallpapers.iter().find(|w| w.id == id) else {
            return;
        };
        w.tags.iter().take(3).map(|t| t.name.clone()).collect::<Vec<_>>().join(" ")
    };
    if query.is_empty() {
        flash_status(ui, "No tags to search similar by");
        return;
    }
    {
        let mut guard = state.lock().unwrap();
        if guard.busy {
            return;
        }
        guard.busy = true;
        guard.query = query.clone();
        guard.page = 1;
        guard.wallpapers.clear();
        guard.page_of.clear();
        guard.positions.clear();
        guard.tile_y.clear();
        guard.decoded.clear();
    }
    start_search(ui, client, handle, state, query, 1, false);
}

/// Copy a URL to the Windows clipboard.
pub fn copy_url(ui: &MainWindow, url: &str) {
    match clipboard_win::set_clipboard_string(url) {
        Ok(()) => flash_status(ui, "URL copied"),
        Err(_) => flash_status(ui, "Copy failed"),
    }
}

/// Handle a right-click context menu action.
pub fn context_action(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
    index: i32,
) {
    let id = ui.get_context_id().to_string();
    match index {
        0 => open_detail(ui, client, handle, state, id),
        1 => toggle_favorite(ui, state, &id),
        2 => apply_wallpaper(ui, client, handle, state, id),
        3 => save_as(ui, client, handle, state, id),
        4 => copy_url(ui, ui.get_context_url().as_ref()),
        _ => {}
    }
}