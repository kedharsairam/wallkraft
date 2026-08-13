//! Masonry grid layout: column balancing and model-of-models construction.

use std::rc::Rc;

use slint::{Image, Model, ModelRc, VecModel};

use crate::model::Wallpaper;
use crate::state::SharedState;
use crate::{MainWindow, WallpaperData};

/// Height per unit width for a wallpaper, clamped so extreme aspect ratios
/// don't blow up the layout. Shared by column balancing and visibility math.
pub fn tile_ratio(w: &Wallpaper) -> f32 {
    let x = w.dimension_x as f32;
    let y = w.dimension_y as f32;
    if x > 0.0 && y > 0.0 {
        (y / x).clamp(0.15, 6.0)
    } else {
        0.625
    }
}

/// Assign each wallpaper to one of `col_count` masonry columns: always place
/// the wallpaper in the currently shortest column so the staggered columns
/// end within roughly one tile of each other. Each tile keeps its true aspect
/// ratio (height = column width * ratio) — no cropping, like Android's grid.
///
/// Returns a column-per-index Vec of WallpaperData (thumbs empty — filled in
/// progressively by start_search).
pub fn build_columns(
    wallpapers: &[Wallpaper],
    col_count: usize,
    favorites: &std::collections::HashSet<String>,
) -> Vec<Vec<WallpaperData>> {
    // Column width used only to estimate heights for balancing (the real
    // width is decided by the layout at runtime).
    const COL_W: f32 = 344.0;

    let mut cols: Vec<Vec<usize>> = vec![Vec::new(); col_count];
    let mut heights = vec![0.0f32; col_count];
    for (i, w) in wallpapers.iter().enumerate() {
        let col = heights
            .iter()
            .enumerate()
            .min_by(|a, b| a.1.total_cmp(b.1))
            .map(|(i, _)| i)
            .unwrap_or(0);
        cols[col].push(i);
        heights[col] += COL_W * tile_ratio(w);
    }

    let to_data = |indices: &[usize]| -> Vec<WallpaperData> {
        indices
            .iter()
            .map(|&i| {
                let wp = &wallpapers[i];
                WallpaperData {
                    id: wp.id.clone().into(),
                    thumb: Image::default(),
                    resolution: wp.resolution.clone().into(),
                    ratio: tile_ratio(wp),
                    favorite: favorites.contains(&wp.id),
                }
            })
            .collect()
    };

    cols.iter().map(|c| to_data(c)).collect()
}

/// (Re)build the `columns` model-of-models on the UI thread. Must be called
/// from the UI thread (it constructs slint::Image values and sets models).
///
/// Thumbs that are already loaded in the current model are carried over by id
/// so a resize that changes the column count does not blank the grid or force
/// re-downloads. The positions map (id -> col,row) is updated in state so
/// in-flight thumbnail fills keep working across re-balances.
pub fn apply_columns(ui: &MainWindow, state: &SharedState, wallpapers: &[Wallpaper]) {
    // Carry over already-loaded thumbs by wallpaper id.
    let mut thumbs: std::collections::HashMap<String, Image> = std::collections::HashMap::new();
    let current = ui.get_columns();
    if let Some(ovm) = current.as_any().downcast_ref::<VecModel<ModelRc<WallpaperData>>>() {
        for ci in 0..ovm.row_count() {
            if let Some(inner) = ovm.row_data(ci)
                && let Some(vm) = inner.as_any().downcast_ref::<VecModel<WallpaperData>>()
            {
                for ri in 0..vm.row_count() {
                    if let Some(d) = vm.row_data(ri)
                        && d.thumb.size().width > 0
                    {
                        thumbs.insert(d.id.to_string(), d.thumb.clone());
                    }
                }
            }
        }
    }

    let col_count = state.lock().unwrap().col_count;
    let favorites = state.lock().unwrap().favorites.clone();
    let columns = build_columns(wallpapers, col_count, &favorites);

    // Refresh the position map so fills land in the right cell even after a
    // re-balance.
    let mut positions: std::collections::HashMap<String, (usize, usize)> =
        std::collections::HashMap::new();
    for (ci, col) in columns.iter().enumerate() {
        for (ri, d) in col.iter().enumerate() {
            positions.insert(d.id.to_string(), (ci, ri));
        }
    }
    let col_width = state.lock().unwrap().col_width;
    let tile_y = compute_tile_y(wallpapers, &positions, col_width);
    {
        let mut guard = state.lock().unwrap();
        guard.positions = positions;
        guard.tile_y = tile_y;
    }

    // Wrap each column in its own VecModel, then wrap the column list.
    let outer: Vec<ModelRc<WallpaperData>> = columns
        .into_iter()
        .map(|col| {
            let col = col
                .into_iter()
                .map(|mut d| {
                    if let Some(img) = thumbs.get(&d.id.to_string()) {
                        d.thumb = img.clone();
                    }
                    d
                })
                .collect::<Vec<_>>();
            ModelRc::new(Rc::new(VecModel::from(col)))
        })
        .collect();
    ui.set_columns(ModelRc::new(Rc::new(VecModel::from(outer))));
}

/// Compute each tile's y-offset (top edge) within its column using the real
/// column width. Powers visibility-based image pruning: given the scroll
/// position, we know exactly which tiles are near the viewport.
pub fn compute_tile_y(
    wallpapers: &[Wallpaper],
    positions: &std::collections::HashMap<String, (usize, usize)>,
    col_width: f32,
) -> std::collections::HashMap<String, f32> {
    const SPACING: f32 = 12.0; // space-3 between tiles
    let mut by_col: std::collections::HashMap<usize, Vec<(usize, &Wallpaper)>> =
        std::collections::HashMap::new();
    for w in wallpapers {
        if let Some((col, row)) = positions.get(&w.id) {
            by_col.entry(*col).or_default().push((*row, w));
        }
    }
    let mut tile_y = std::collections::HashMap::new();
    for (_, mut tiles) in by_col {
        tiles.sort_by_key(|(row, _)| *row);
        let mut y = 0.0f32;
        for (_, w) in tiles {
            tile_y.insert(w.id.clone(), y);
            y += col_width * tile_ratio(w) + SPACING;
        }
    }
    tile_y
}

/// Recompute tile y-offsets after a resize that changed the column width but
/// not the column count (tile heights changed, so visibility math must too).
pub fn update_tile_y(state: &SharedState) {
    let (wallpapers, positions, col_width) = {
        let guard = state.lock().unwrap();
        (guard.wallpapers.clone(), guard.positions.clone(), guard.col_width)
    };
    let tile_y = compute_tile_y(&wallpapers, &positions, col_width);
    state.lock().unwrap().tile_y = tile_y;
}

#[cfg(test)]
mod tests {
    use super::{build_columns, compute_tile_y};
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

    #[test]
    fn empty_input_yields_empty_columns() {
        let cols = build_columns(&[], 4, &Default::default());
        assert_eq!(cols.len(), 4);
        assert!(cols.iter().all(|c| c.is_empty()));
    }

    #[test]
    fn every_wallpaper_lands_in_exactly_one_column() {
        let wallpapers: Vec<Wallpaper> = (0..10).map(|i| wp(&format!("w{i}"), 1920, 1080)).collect();
        let cols = build_columns(&wallpapers, 3, &Default::default());
        let total: usize = cols.iter().map(|c| c.len()).sum();
        assert_eq!(total, 10);
        // No duplicates: every id appears exactly once across all columns.
        let mut ids: Vec<String> = cols
            .iter()
            .flat_map(|c| c.iter().map(|d| d.id.to_string()))
            .collect();
        ids.sort();
        ids.dedup();
        assert_eq!(ids.len(), 10);
    }

    #[test]
    fn columns_are_balanced_within_one_tile() {
        // All tiles have the same ratio, so a perfect round-robin is expected.
        let wallpapers: Vec<Wallpaper> = (0..9).map(|i| wp(&format!("w{i}"), 1920, 1080)).collect();
        let cols = build_columns(&wallpapers, 3, &Default::default());
        let lens: Vec<usize> = cols.iter().map(|c| c.len()).collect();
        let min = *lens.iter().min().unwrap();
        let max = *lens.iter().max().unwrap();
        assert!(max - min <= 1, "column lengths too uneven: {lens:?}");
    }

    #[test]
    fn tile_ratio_preserves_aspect() {
        // Portrait wallpaper (1080x1920) must have ratio > 1 (taller than wide).
        let portrait = build_columns(&[wp("p", 1080, 1920)], 2, &Default::default());
        assert!(portrait[0][0].ratio > 1.0);
        // Landscape (1920x1080) must have ratio < 1.
        let landscape = build_columns(&[wp("l", 1920, 1080)], 2, &Default::default());
        assert!(landscape[0][0].ratio < 1.0);
        // Square stays ~1.
        let square = build_columns(&[wp("s", 1000, 1000)], 2, &Default::default());
        assert!((square[0][0].ratio - 1.0).abs() < 0.01);
    }

    #[test]
    fn column_count_respected() {
        let wallpapers: Vec<Wallpaper> = (0..8).map(|i| wp(&format!("w{i}"), 1920, 1080)).collect();
        assert_eq!(build_columns(&wallpapers, 2, &Default::default()).len(), 2);
        assert_eq!(build_columns(&wallpapers, 5, &Default::default()).len(), 5);
        assert_eq!(build_columns(&wallpapers, 8, &Default::default()).len(), 8);
    }

    #[test]
    fn tile_y_offsets_are_cumulative_per_column() {
        // 3 identical landscape tiles in one column: each is col_width * 0.5625
        // tall plus 12px spacing, so offsets are 0, h, 2h.
        let wallpapers: Vec<Wallpaper> = (0..3).map(|i| wp(&format!("w{i}"), 1920, 1080)).collect();
        let positions = [(0usize, 0usize), (0, 1), (0, 2)]
            .into_iter()
            .enumerate()
            .map(|(i, (c, r))| (format!("w{i}"), (c, r)))
            .collect();
        let tile_y = compute_tile_y(&wallpapers, &positions, 400.0);
        let h = 400.0 * 0.5625 + 12.0;
        assert_eq!(tile_y["w0"], 0.0);
        assert!((tile_y["w1"] - h).abs() < 0.001);
        assert!((tile_y["w2"] - 2.0 * h).abs() < 0.001);
    }
}
