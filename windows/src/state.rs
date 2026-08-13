//! Shared application state and layout math.

use std::collections::{HashMap, HashSet};
use std::path::PathBuf;
use std::sync::{Arc, Mutex};

use crate::api::Filters;
use crate::grid::tile_ratio;
use crate::model::Wallpaper;
use crate::storage;

/// Max pages of results kept in memory. Older pages are dropped as the user
/// scrolls, so the model stays bounded no matter how far they go.
pub const MAX_PAGES: u32 = 5;
/// Safety cap on decoded tile images in the model. Structurally guaranteed by
/// MAX_PAGES (5 × 24 = 120 < 200); kept as a constant so relaxing the page cap
/// later can't silently unbounded decoded memory.
pub const MAX_DECODED: usize = 200;
/// Tiles this many viewports above the viewport top get their image pruned.
pub const KEEP_ABOVE_VIEWPORTS: f32 = 1.5;
/// Tiles this many viewports below the viewport bottom get their image pruned.
pub const KEEP_BELOW_VIEWPORTS: f32 = 2.5;

/// Shared app state, mutated on the UI thread and read by background tasks.
pub struct AppState {
    pub query: String,
    pub page: u32,
    pub wallpapers: Vec<Wallpaper>,
    pub filters: Filters,
    pub busy: bool,
    /// Current masonry column count (recomputed on window resize).
    pub col_count: usize,
    /// Current masonry column width in px (recomputed on window resize).
    pub col_width: f32,
    /// wallpaper id -> (column, row) in the live `columns` model. Kept in
    /// state (not captured at spawn) so an in-flight thumbnail fill lands in
    /// the right cell even if a resize re-balanced the grid meanwhile.
    pub positions: HashMap<String, (usize, usize)>,
    /// wallpaper id -> page it was loaded from (drives page capping).
    pub page_of: HashMap<String, u32>,
    /// wallpaper id -> y-offset (px) of the tile's top edge within its column.
    /// Powers visibility-based image pruning (which tiles are near the viewport).
    pub tile_y: HashMap<String, f32>,
    /// Current scroll offset in px (0 at top, positive when scrolled down).
    pub scroll_y: f32,
    /// Visible height of the scroll area in px.
    pub visible_height: f32,
    /// ids of tiles that currently hold a decoded image in the model.
    pub decoded: HashSet<String>,
    /// ids of favorited wallpapers (quick heart-state lookups).
    pub favorites: HashSet<String>,
    /// Full metadata of favorited wallpapers, newest first (Favorites view).
    pub favorite_wallpapers: Vec<Wallpaper>,
    /// Absolute paths of recently applied wallpapers, newest first (Undo).
    pub history: Vec<String>,
    /// Index into `WallpaperStyle::ALL` used when applying.
    pub wallpaper_style: u32,
    /// "dark" | "light" | "system".
    pub theme: String,
    /// Whether the slideshow is running.
    pub slideshow_active: bool,
    /// Seconds between slideshow steps.
    pub slideshow_interval_secs: u64,
    /// Index of the next wallpaper the slideshow will apply (cycles the grid).
    pub slideshow_index: usize,
    /// Which collection the grid is showing.
    pub view: View,
}

/// What the main grid is currently displaying.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum View {
    /// Live search results.
    Grid,
    /// Locally favorited wallpapers.
    Favorites,
}

pub type SharedState = Arc<Mutex<AppState>>;

/// How many masonry columns fit a window width. Tiles target ~300px wide; we
/// never drop below 2 (narrow windows) or exceed 8 (ultra-wide, so tiles stay
/// reasonable instead of stretching wall-to-wall).
pub fn column_count_for(width: f32) -> usize {
    const MIN_TILE: f32 = 300.0;
    const PADDING: f32 = 32.0; // 16px each side
    const SPACING: f32 = 12.0; // space-3 between columns
    let usable = width - PADDING;
    let n = ((usable + SPACING) / (MIN_TILE + SPACING)).floor() as usize;
    n.clamp(2, 8)
}

/// Width of each masonry column for a window width and column count. Mirrors
/// the layout: 16px padding each side, 12px spacing between columns, columns
/// share the rest equally. Used to compute tile heights for visibility math.
pub fn column_width_for(window_width: f32, col_count: usize) -> f32 {
    const PADDING: f32 = 32.0; // 16px each side
    const SPACING: f32 = 12.0; // space-3 between columns
    let usable = window_width - PADDING;
    (usable - SPACING * (col_count as f32 - 1.0)) / col_count as f32
}

/// Drop the oldest pages beyond MAX_PAGES. Returns the height (px) of the
/// dropped content in the column that lost the most, so the caller can
/// compensate the scroll position and keep the view stable (masonry columns
/// are balanced within one tile, so the max is within a tile of the others).
pub fn cap_pages(state: &mut AppState) -> f32 {
    let mut pages: Vec<u32> = state.page_of.values().copied().collect();
    pages.sort_unstable();
    pages.dedup();
    let mut dropped_height = 0.0f32;
    while pages.len() > MAX_PAGES as usize {
        let oldest = pages.remove(0);
        let dropped: Vec<Wallpaper> = state
            .wallpapers
            .iter()
            .filter(|w| state.page_of.get(&w.id) == Some(&oldest))
            .cloned()
            .collect();
        let dropped_ids: HashSet<String> = dropped.iter().map(|w| w.id.clone()).collect();
        // Height lost per column; the max drives scroll compensation.
        let mut per_col: HashMap<usize, f32> = HashMap::new();
        for w in &dropped {
            if let Some((col, _)) = state.positions.get(&w.id) {
                *per_col.entry(*col).or_insert(0.0) += state.col_width * tile_ratio(w);
            }
        }
        dropped_height = dropped_height.max(per_col.values().copied().fold(0.0, f32::max));
        state.wallpapers.retain(|w| !dropped_ids.contains(&w.id));
        state.page_of.retain(|id, _| !dropped_ids.contains(id));
        state.positions.retain(|id, _| !dropped_ids.contains(id));
        state.tile_y.retain(|id, _| !dropped_ids.contains(id));
        state.decoded.retain(|id| !dropped_ids.contains(id));
    }
    dropped_height
}

/// Resolve the app cache directory, falling back to a temp dir on failure.
pub fn cache() -> PathBuf {
    storage::cache_dir().unwrap_or_else(|_| std::env::temp_dir().join("wallkraft"))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::model::{Thumbs, Wallpaper};

    fn wp(id: &str, x: u32, y: u32) -> Wallpaper {
        Wallpaper {
            id: id.into(),
            url: format!("https://wallhaven.cc/w/{id}"),
            path: format!("https://w.wallhaven.cc/full/{id}.jpg"),
            resolution: format!("{x}x{y}"),
            dimension_x: x,
            dimension_y: y,
            thumbs: Thumbs {
                large: String::new(),
                original: String::new(),
            },
            views: 0,
            favorites: 0,
            purity: "sfw".into(),
            category: "general".into(),
            colors: Vec::new(),
            tags: Vec::new(),
            user: None,
        }
    }

    fn state_with_pages(pages: u32, per_page: usize) -> AppState {
        let mut state = AppState {
            query: String::new(),
            page: pages,
            wallpapers: Vec::new(),
            filters: Filters::default(),
            busy: false,
            col_count: 3,
            col_width: 344.0,
            positions: HashMap::new(),
            page_of: HashMap::new(),
            tile_y: HashMap::new(),
            scroll_y: 0.0,
            visible_height: 700.0,
            decoded: HashSet::new(),
            favorites: HashSet::new(),
            favorite_wallpapers: Vec::new(),
            history: Vec::new(),
            wallpaper_style: 0,
            theme: "dark".into(),
            slideshow_active: false,
            slideshow_interval_secs: 30,
            slideshow_index: 0,
            view: View::Grid,
        };
        for p in 1..=pages {
            for i in 0..per_page {
                let id = format!("p{p}-w{i}");
                state.wallpapers.push(wp(&id, 1920, 1080));
                state.page_of.insert(id.clone(), p);
                state.positions.insert(id, (i % 3, i / 3));
            }
        }
        state
    }

    #[test]
    fn column_count_clamps_to_minimum() {
        // Narrow windows never drop below 2 columns.
        assert_eq!(column_count_for(0.0), 2);
        assert_eq!(column_count_for(200.0), 2);
        assert_eq!(column_count_for(600.0), 2);
    }

    #[test]
    fn column_count_scales_with_width() {
        assert_eq!(column_count_for(1100.0), 3);
        assert_eq!(column_count_for(1366.0), 4);
        assert_eq!(column_count_for(1920.0), 6);
    }

    #[test]
    fn column_count_caps_at_maximum() {
        // Ultra-wide screens never exceed 8 columns.
        assert_eq!(column_count_for(2560.0), 8);
        assert_eq!(column_count_for(5120.0), 8);
    }

    #[test]
    fn column_width_matches_layout_math() {
        // 1100px window, 3 columns: (1100 - 32 - 2*12) / 3 = 348.
        assert_eq!(column_width_for(1100.0, 3), 348.0);
        // 1 column: full usable width.
        assert_eq!(column_width_for(1100.0, 1), 1068.0);
    }

    #[test]
    fn cap_pages_keeps_newest_five() {
        let mut state = state_with_pages(6, 4);
        let dropped = cap_pages(&mut state);
        assert!(dropped > 0.0);
        let pages: HashSet<u32> = state.page_of.values().copied().collect();
        assert_eq!(pages.len(), MAX_PAGES as usize);
        assert!(!pages.contains(&1), "oldest page must be dropped");
        assert!(pages.contains(&6), "newest page must survive");
        assert_eq!(state.wallpapers.len(), 20);
        // Dropped ids are gone from every index.
        assert!(!state.positions.keys().any(|id| id.starts_with("p1-")));
        assert!(!state.tile_y.keys().any(|id| id.starts_with("p1-")));
        assert!(!state.decoded.iter().any(|id| id.starts_with("p1-")));
    }

    #[test]
    fn cap_pages_noop_under_limit() {
        let mut state = state_with_pages(3, 4);
        let dropped = cap_pages(&mut state);
        assert_eq!(dropped, 0.0);
        assert_eq!(state.wallpapers.len(), 12);
    }
}