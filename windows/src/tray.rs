//! System tray icon with a small menu (Show/Hide, Random, Undo, Quit).
//!
//! The `tray-icon` crate posts events to static channels; we poll them from a
//! repeating `slint::Timer` on the UI thread (the Slint event loop owns the
//! winit loop, so we can't use an event-loop proxy). The icon and timer are
//! intentionally leaked — both must outlive the main loop.

use slint::ComponentHandle;

use crate::api;
use crate::state::SharedState;
use crate::ui;
use crate::MainWindow;

/// Create the tray icon and start polling its menu/event channels.
pub fn setup(
    ui: &MainWindow,
    client: &api::WallhavenClient,
    handle: &tokio::runtime::Handle,
    state: &SharedState,
) {
    use tray_icon::menu::{Menu, MenuEvent, MenuItem, PredefinedMenuItem};
    use tray_icon::{TrayIconBuilder, TrayIconEvent};

    let menu = Menu::new();
    let toggle = MenuItem::new("Show / Hide", true, None);
    let random = MenuItem::new("Random wallpaper", true, None);
    let undo = MenuItem::new("Undo wallpaper", true, None);
    let quit = MenuItem::new("Quit", true, None);
    for item in [&toggle, &random, &undo] {
        if let Err(e) = menu.append(item) {
            tracing::error!("tray menu: {e}");
        }
    }
    if let Err(e) = menu.append(&PredefinedMenuItem::separator()) {
        tracing::error!("tray menu: {e}");
    }
    if let Err(e) = menu.append(&quit) {
        tracing::error!("tray menu: {e}");
    }

    let toggle_id = toggle.id().clone();
    let random_id = random.id().clone();
    let undo_id = undo.id().clone();
    let quit_id = quit.id().clone();

    let tray = match TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip("WallKraft")
        .with_icon(build_icon())
        .build()
    {
        Ok(t) => t,
        Err(e) => {
            tracing::warn!("tray icon unavailable: {e}");
            return;
        }
    };
    // Keep the icon alive for the whole app lifetime.
    Box::leak(Box::new(tray));

    let ui_weak = ui.as_weak();
    let client = client.clone();
    let handle = handle.clone();
    let state = state.clone();
    let timer = slint::Timer::default();
    timer.start(
        slint::TimerMode::Repeated,
        std::time::Duration::from_millis(250),
        move || {
            while let Ok(event) = MenuEvent::receiver().try_recv() {
                if event.id == toggle_id {
                    if let Some(ui) = ui_weak.upgrade() {
                        if ui.window().is_visible() {
                            let _ = ui.window().hide();
                        } else {
                            let _ = ui.window().show();
                        }
                    }
                } else if event.id == random_id {
                    if let Some(ui) = ui_weak.upgrade() {
                        ui::random_search(&ui, &client, &handle, &state);
                    }
                } else if event.id == undo_id {
                    if let Some(ui) = ui_weak.upgrade() {
                        ui::undo(&ui, &state);
                    }
                } else if event.id == quit_id {
                    std::process::exit(0);
                }
            }
            while let Ok(TrayIconEvent::DoubleClick { .. }) = TrayIconEvent::receiver().try_recv() {
                if let Some(ui) = ui_weak.upgrade() {
                    let _ = ui.window().show();
                }
            }
        },
    );
    // Keep the poll timer alive for the whole app lifetime.
    Box::leak(Box::new(timer));
}

/// Generate a simple app icon: an accent-blue disc on the dark surface color.
fn build_icon() -> tray_icon::Icon {
    const SIZE: u32 = 32;
    const CX: f32 = 15.5;
    const CY: f32 = 15.5;
    const RADIUS: f32 = 11.0;
    let mut rgba = Vec::with_capacity((SIZE * SIZE * 4) as usize);
    for y in 0..SIZE {
        for x in 0..SIZE {
            let dx = x as f32 + 0.5 - CX;
            let dy = y as f32 + 0.5 - CY;
            let inside = dx * dx + dy * dy <= RADIUS * RADIUS;
            if inside {
                rgba.extend_from_slice(&[0x0A, 0x84, 0xFF, 0xFF]); // accent
            } else {
                rgba.extend_from_slice(&[0x1C, 0x1C, 0x1E, 0xFF]); // surface
            }
        }
    }
    tray_icon::Icon::from_rgba(rgba, SIZE, SIZE).expect("tray icon")
}
