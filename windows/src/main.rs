//! WallKraft for Windows — entry point and UI wiring.

mod api;
mod drag;
mod grid;
mod images;
mod logging;
mod model;
mod state;
mod storage;
mod tray;
mod ui;
mod update;
mod wallpaper;

use std::sync::{Arc, Mutex};

use anyhow::Result;
use slint::{ComponentHandle, ModelRc, SharedString, VecModel};

use crate::api::Filters;
use crate::grid::{apply_columns, update_tile_y};
use crate::state::{column_count_for, column_width_for, AppState, SharedState, View};
use crate::ui::{
    apply_theme, apply_wallpaper, atleast_changed, color_changed, context_action, copy_url,
    flash_status, on_viewport_changed, open_detail, open_folder, random_search, ratio_changed,
    reload, save_as, show_favorites, similar, slideshow_interval_changed, slideshow_toggle,
    start_search, style_changed, theme_changed, toggle_favorite, undo,
};

slint::include_modules!();

/// Option index → API value mapping, aligned with the Slint dropdown models.
const CATEGORY_CODES: [&str; 4] = ["100", "010", "001", "111"];
const PURITY_CODES: [&str; 3] = ["100", "110", "111"];
const SORTING_CODES: [&str; 5] = ["date_added", "relevance", "views", "random", "favorites"];
const ORIENTATION_CODES: [&str; 3] = ["", "landscape", "portrait"];

fn main() -> Result<()> {
    logging::init()?;
    tracing::info!("WallKraft starting");
    // Enforce the cache size cap before the UI comes up (best-effort).
    let cache = storage::cache_dir()?;
    storage::evict_if_needed(&cache);
    // OLE must be initialized on the UI thread before any tile drag-out.
    drag::ensure_ole();
    let rt = tokio::runtime::Runtime::new()?;
    let client = api::WallhavenClient::new(None)?;
    let ui = MainWindow::new()?;

    // Load persisted settings, favorites, and history before the UI shows.
    let settings = storage::load_settings(&cache);
    let favorites = storage::load_favorites(&cache).unwrap_or_default();
    let history = storage::load_history(&cache).unwrap_or_default();
    let undo_visible = history.len() >= 2;
    let favorite_ids: std::collections::HashSet<String> =
        favorites.iter().map(|w| w.id.clone()).collect();

    let state: SharedState = Arc::new(Mutex::new(AppState {
        query: String::new(),
        page: 1,
        wallpapers: Vec::new(),
        filters: Filters::default(),
        busy: false,
        col_count: column_count_for(1100.0),
        col_width: column_width_for(1100.0, column_count_for(1100.0)),
        positions: std::collections::HashMap::new(),
        page_of: std::collections::HashMap::new(),
        tile_y: std::collections::HashMap::new(),
        scroll_y: 0.0,
        visible_height: 0.0,
        decoded: std::collections::HashSet::new(),
        favorites: favorite_ids,
        favorite_wallpapers: favorites,
        history,
        wallpaper_style: settings.wallpaper_style,
        theme: settings.theme.clone(),
        slideshow_active: false,
        slideshow_interval_secs: settings.slideshow_interval_secs,
        slideshow_index: 0,
        view: View::Grid,
    }));

    // Reflect persisted settings in the UI.
    let theme_index = match settings.theme.as_str() {
        "light" => 1,
        "system" => 2,
        _ => 0,
    };
    ui.set_theme_index(theme_index);
    ui.set_style_index(settings.wallpaper_style as i32);
    ui.set_slideshow_interval_index(match settings.slideshow_interval_secs {
        10 => 0,
        60 => 2,
        300 => 3,
        _ => 1,
    });
    ui.set_undo_visible(undo_visible);
    ui.set_skeleton_columns(column_count_for(1100.0) as i32);
    apply_theme(&ui, &settings.theme);

    // Search
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_search(move |query| {
            let query = query.as_str().trim().to_string();
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
                let mut guard = state.lock().unwrap();
                if guard.busy {
                    return;
                }
                guard.busy = true;
                (guard.query.clone(), guard.page + 1)
            };
            start_search(&ui_cb, &client, &handle, &state, query, page, true);
        });
    }

    // Retry after a network error — re-runs the current query from page 1.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_retry(move || {
            let query = {
                let mut guard = state.lock().unwrap();
                if guard.busy {
                    return;
                }
                guard.busy = true;
                guard.page = 1;
                guard.query.clone()
            };
            start_search(&ui_cb, &client, &handle, &state, query, 1, false);
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

    // Open in browser
    ui.on_open_wallhaven(move |url| {
        let url = url.as_str();
        let _ = std::process::Command::new("cmd")
            .args(["/C", "start", "", url])
            .spawn();
    });

    // Filter changes re-run the current search from page 1.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_category_changed(move |i| {
            {
                let mut guard = state.lock().unwrap();
                guard.filters.categories = CATEGORY_CODES[i as usize].to_string();
            }
            ui_cb.set_category_index(i);
            reload(&ui_cb, &client, &handle, &state);
        });
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_purity_changed(move |i| {
            {
                let mut guard = state.lock().unwrap();
                guard.filters.purity = PURITY_CODES[i as usize].to_string();
            }
            ui_cb.set_purity_index(i);
            reload(&ui_cb, &client, &handle, &state);
        });
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_sorting_changed(move |i| {
            {
                let mut guard = state.lock().unwrap();
                guard.filters.sorting = SORTING_CODES[i as usize].to_string();
            }
            ui_cb.set_sorting_index(i);
            reload(&ui_cb, &client, &handle, &state);
        });
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_orientation_changed(move |i| {
            {
                let mut guard = state.lock().unwrap();
                guard.filters.orientation = ORIENTATION_CODES[i as usize].to_string();
            }
            ui_cb.set_orientation_index(i);
            reload(&ui_cb, &client, &handle, &state);
        });
    }

    // Favorites toggle (from tile heart or detail view).
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_toggle_favorite(move |id| toggle_favorite(&ui_cb, &state, id.as_str()));
    }

    // Favorites view toggle.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_show_favorites(move || show_favorites(&ui_cb, &state));
    }

    // Random search.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_random_search(move || random_search(&ui_cb, &client, &handle, &state));
    }

    // Color filter.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_color_changed(move |text| color_changed(&ui_cb, &client, &handle, &state, text.as_str()));
    }

    // Minimum resolution filter.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_atleast_changed(move |i| atleast_changed(&ui_cb, &client, &handle, &state, i));
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_ratio_changed(move |i| ratio_changed(&ui_cb, &client, &handle, &state, i));
    }

    // Theme segmented control.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_theme_changed(move |i| theme_changed(&ui_cb, &state, i));
    }

    // Wallpaper style dropdown.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_style_changed(move |i| style_changed(&ui_cb, &state, i));
    }

    // Slideshow toggle + interval.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_slideshow_toggle(move || slideshow_toggle(&ui_cb, &client, &handle, &state));
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_slideshow_interval_changed(move |i| {
            slideshow_interval_changed(&ui_cb, &client, &handle, &state, i)
        });
    }

    // Undo last wallpaper.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_undo(move || undo(&ui_cb, &state));
    }

    // Detail-view actions.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_save_as(move |id| save_as(&ui_cb, &client, &handle, &state, id.as_str().to_string()));
    }
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_open_folder(move |id| open_folder(&ui_cb, &state, id.as_str()));
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_similar(move |id| similar(&ui_cb, &client, &handle, &state, id.as_str()));
    }
    {
        let ui_cb = ui.clone_strong();
        ui.on_copy_url(move |url| copy_url(&ui_cb, url.as_str()));
    }

    // Right-click context menu on a tile.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_context_menu(move |id| {
            let (fav, url) = {
                let guard = state.lock().unwrap();
                let fav = guard.favorites.contains(id.as_str());
                let url = guard
                    .wallpapers
                    .iter()
                    .find(|w| id == w.id)
                    .map(|w| w.url.clone())
                    .unwrap_or_default();
                (fav, url)
            };
            ui_cb.set_context_id(id.clone());
            ui_cb.set_context_url(url.into());
            let items: Vec<SharedString> = vec![
                "Open".into(),
                if fav { "Unfavorite".into() } else { "Favorite".into() },
                "Set as Wallpaper".into(),
                "Save As".into(),
                "Copy URL".into(),
            ];
            ui_cb.set_context_items(ModelRc::new(VecModel::from(items)));
        });
    }
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_context_action(move |i| context_action(&ui_cb, &client, &handle, &state, i));
    }

    // Drag a tile out to the desktop/folder to save the full-res image.
    // If the full-res isn't cached yet, download it first — a drag that's
    // already in progress can't be started after the fact, so we tell the
    // user to drag again once it's ready.
    {
        let ui_weak = ui.as_weak();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        let cache = cache.clone();
        ui.on_start_drag(move |id| {
            let id = id.as_str().to_string();
            let (url, full_dest) = {
                let guard = state.lock().unwrap();
                let Some(w) = guard.wallpapers.iter().find(|w| w.id == id).cloned() else {
                    return;
                };
                let full_dest = storage::full_path(&cache, &w.id);
                (w.path, full_dest)
            };
            if full_dest.exists() {
                // Runs on the UI thread (callback from the UI), so OLE drag is fine.
                drag::start_drag(&full_dest);
            } else {
                let ui_weak = ui_weak.clone();
                let client = client.clone();
                handle.spawn(async move {
                    let _ = ui_weak.upgrade_in_event_loop(move |ui| {
                        flash_status(&ui, "Downloading full-res…")
                    });
                    let ok = client.download_to(&url, &full_dest).await.is_ok();
                    let _ = ui_weak.upgrade_in_event_loop(move |ui| {
                        if ok {
                            flash_status(&ui, "Ready — drag again to save");
                        } else {
                            flash_status(&ui, "Download failed");
                        }
                    });
                });
            }
        });
    }

    // Scroll tracking: prune/decode tile images so decoded memory stays
    // bounded to the viewport neighborhood as the user scrolls.
    {
        let ui_cb = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        ui.on_viewport_changed(move |scroll_y: f32, visible_height: f32| {
            on_viewport_changed(&ui_cb, &client, &handle, &state, scroll_y, visible_height);
        });
    }

    // Window resize: recompute how many columns fit and re-balance the grid.
    // `width` is in px (logical). The column width is tracked on every resize
    // (tile heights depend on it); the grid is only rebuilt when the column
    // count actually changes.
    {
        let ui_cb = ui.clone_strong();
        let state = state.clone();
        ui.on_columns_changed(move |width: f32| {
            let new_count = column_count_for(width);
            // Keep the first-load skeleton's column count in sync.
            ui_cb.set_skeleton_columns(new_count as i32);
            let changed = {
                let mut guard = state.lock().unwrap();
                guard.col_width = column_width_for(width, new_count);
                if guard.col_count != new_count {
                    guard.col_count = new_count;
                    true
                } else {
                    false
                }
            };
            if changed {
                // Runs on the UI thread (callback from the UI), so it's safe
                // to build WallpaperData (holds slint::Image) and set models.
                let wallpapers = {
                    let guard = state.lock().unwrap();
                    guard.wallpapers.clone()
                };
                apply_columns(&ui_cb, &state, &wallpapers);
            } else {
                // Column count unchanged but width changed → tile heights
                // changed, so refresh the y-offsets used for pruning.
                update_tile_y(&state);
            }
        });
    }

    // Initial load once the event loop starts. With an empty query the API
    // now returns the latest uploads (sorting=date_added), so the grid
    // populates immediately — parity with the Android app's launch view.
    {
        let ui = ui.clone_strong();
        let client = client.clone();
        let handle = rt.handle().clone();
        let state = state.clone();
        slint::Timer::single_shot(std::time::Duration::ZERO, move || {
            state.lock().unwrap().busy = true;
            start_search(&ui, &client, &handle, &state, String::new(), 1, false);
        });
    }

    // System tray icon (Show/Hide, Random, Undo, Quit). Must be created before
    // the event loop starts; it spawns its own hidden window and processes
    // messages once the loop is running.
    tray::setup(&ui, &client, rt.handle(), &state);

    // Auto-update: check GitHub releases on startup (background, fail-silent).
    // Surfaces an "Update vX.Y.Z" button in the header when a newer release
    // exists; the button triggers download + silent install + relaunch.
    {
        let ui_weak = ui.as_weak();
        rt.spawn(update::check_for_update(ui_weak));
    }
    {
        let ui_weak = ui.as_weak();
        ui.on_update_now(move |version| {
            update::apply_update(ui_weak.clone(), version.to_string(), rt.handle());
        });
    }

    ui.run()?;
    Ok(())
}