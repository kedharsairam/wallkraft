//! WallKraft for Windows — entry point and UI wiring.

mod api;
mod model;
mod storage;
mod wallpaper;

use std::path::PathBuf;
use std::rc::Rc;
use std::sync::{Arc, Mutex};

use anyhow::Result;
use model::Wallpaper;
use slint::{ComponentHandle, Image, ModelRc, SharedString, VecModel, Weak};

slint::include_modules!();

/// Shared app state, mutated on the UI thread and read by background tasks.
struct AppState {
    query: String,
    page: u32,
    wallpapers: Vec<Wallpaper>,
}

type SharedState = Arc<Mutex<AppState>>;

fn cache() -> PathBuf {
    storage::cache_dir().unwrap_or_else(|_| std::env::temp_dir().join("wallkraft"))
}

fn main() -> Result<()> {
    let rt = tokio::runtime::Runtime::new()?;
    let client = api::WallhavenClient::new(None)?;
    let ui = MainWindow::new()?;
    let state: SharedState = Arc::new(Mutex::new(AppState {
        query: String::new(),
        page: 1,
        wallpapers: Vec::new(),
    }));

    // Search
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_search(move |query| {
            let query = query.as_str().trim().to_string();
            state.lock().unwrap().query = query.clone();
            state.lock().unwrap().page = 1;
            state.lock().unwrap().wallpapers.clear();
            start_search(&ui_cb, &client, &handle, &state, query, 1, false);
        });
    }

    // Load more
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_load_more(move || {
            let (query, page) = {
                let guard = state.lock().unwrap();
                (guard.query.clone(), guard.page + 1)
            };
            start_search(&ui_cb, &client, &handle, &state, query, page, true);
        });
    }

    // Open detail
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_open_detail(move |id| {
            open_detail(&ui_cb, &client, &handle, &state, id.as_str().to_string());
        });
    }

    // Set wallpaper
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_set_wallpaper(move |id| {
            apply_wallpaper(&ui_cb, &client, &handle, &state, id.as_str().to_string());
        });
    }

    // Close detail
    {
        let ui_cb = ui.clone_strong();
        ui.on_close_detail(move || ui_cb.set_detail_visible(false));
    }

    // Initial load once the event loop starts
    {
        let ui = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        slint::Timer::single_shot(std::time::Duration::ZERO, move || {
            start_search(&ui, &client, &handle, &state, String::new(), 1, false);
        });
    }

    ui.run()?;
    Ok(())
}

/// Fetch a page of search results, download thumbnails, and update the grid.
fn start_search(
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
    handle.spawn(async move {
        match client.search(&query, page).await {
            Ok(mut wallpapers) => {
                for w in &wallpapers {
                    let dest = storage::thumb_path(&cache, &w.id);
                    if !dest.exists() {
                        let _ = client.download_to(&w.thumbs.small, &dest).await;
                    }
                }
                {
                    let mut guard = state.lock().unwrap();
                    if append {
                        guard.wallpapers.append(&mut wallpapers);
                    } else {
                        guard.wallpapers = wallpapers;
                    }
                    guard.page = page;
                }
                // Images are loaded on the UI thread (slint::Image is not Send).
                let state_ui = state.clone();
                let cache_ui = cache.clone();
                let _ = ui_weak.upgrade_in_event_loop(move |ui| {
                    let wallpapers = state_ui.lock().unwrap().wallpapers.clone();
                    let rows = build_rows(&wallpapers, &cache_ui);
                    ui.set_rows(ModelRc::new(Rc::new(VecModel::from(rows))));
                    ui.set_loading(false);
                    ui.set_status(SharedString::default());
                });
            }
            Err(e) => {
                let msg = format!("Search failed: {e}");
                let _ = ui_weak.upgrade_in_event_loop(move |ui| {
                    ui.set_loading(false);
                    ui.set_status(msg.into());
                });
            }
        }
    });
}

/// Show the detail view for a wallpaper, downloading the full-res image first.
fn open_detail(
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
        let wallpaper = {
            let guard = state.lock().unwrap();
            guard.wallpapers.iter().find(|w| w.id == id).cloned()
        };
        let Some(w) = wallpaper else { return };
        let dest = storage::full_path(&cache, &w.id);
        if !dest.exists() && client.download_to(&w.path, &dest).await.is_err() {
            let msg = format!("Download failed for {}", w.id);
            let _ = ui_weak.upgrade_in_event_loop(move |ui| ui.set_status(msg.into()));
            return;
        }
        let resolution = w.resolution.clone();
        let id = w.id.clone();
        // Image is loaded on the UI thread (slint::Image is not Send).
        let _ = ui_weak.upgrade_in_event_loop(move |ui| {
            let image = Image::load_from_path(&dest).unwrap_or_default();
            ui.set_detail_image(image);
            ui.set_detail_resolution(resolution.into());
            ui.set_detail_id(id.into());
            ui.set_detail_visible(true);
            ui.set_status(SharedString::default());
        });
    });
}

/// Download the full-res image (if needed) and apply it as the desktop wallpaper.
fn apply_wallpaper(
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
        let wallpaper = {
            let guard = state.lock().unwrap();
            guard.wallpapers.iter().find(|w| w.id == id).cloned()
        };
        let Some(w) = wallpaper else { return };
        let dest = storage::full_path(&cache, &w.id);
        if !dest.exists() && client.download_to(&w.path, &dest).await.is_err() {
            let msg = format!("Download failed for {}", w.id);
            let _ = ui_weak.upgrade_in_event_loop(move |ui| ui.set_status(msg.into()));
            return;
        }
        let msg = match wallpaper::set_wallpaper(&dest) {
            Ok(()) => "Wallpaper set".to_string(),
            Err(e) => format!("Failed to set wallpaper: {e}"),
        };
        let _ = ui_weak.upgrade_in_event_loop(move |ui| ui.set_status(msg.into()));
    });
}

/// Chunk the flat wallpaper list into rows of two for the grid.
fn build_rows(wallpapers: &[Wallpaper], cache: &PathBuf) -> Vec<RowData> {
    wallpapers
        .chunks(2)
        .map(|chunk| RowData {
            left: wallpaper_data(&chunk[0], cache),
            right: chunk.get(1).map(|w| wallpaper_data(w, cache)).unwrap_or_default(),
        })
        .collect()
}

fn wallpaper_data(w: &Wallpaper, cache: &PathBuf) -> WallpaperData {
    let thumb = storage::thumb_path(cache, &w.id);
    WallpaperData {
        id: w.id.clone().into(),
        thumb: Image::load_from_path(&thumb).unwrap_or_default(),
        resolution: w.resolution.clone().into(),
        category: w.category.clone().into(),
    }
}