//! Local storage: cache directory for downloaded images.

use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

/// Resolve (and create) the app cache directory.
pub fn cache_dir() -> Result<PathBuf> {
    let dir = directories::ProjectDirs::from("com", "wallkraft", "WallKraft")
        .context("failed to resolve app data directory")?
        .cache_dir()
        .to_path_buf();
    std::fs::create_dir_all(&dir)?;
    Ok(dir)
}

/// Cache file for a grid thumbnail. We cache `thumbs.original` downscaled to
/// <=384px (aspect preserved) — the 432px `large` thumb is a fixed 16:9 crop,
/// which zoomed every tile. Lives under `thumbs/grid3`; the dir name is a
/// cache-invalidation key for thumbs cached by older builds.
pub fn thumb_path(cache: &std::path::Path, id: &str) -> PathBuf {
    let dir = cache.join("thumbs").join("grid3");
    std::fs::create_dir_all(&dir).ok();
    dir.join(format!("{id}.jpg"))
}

/// Cache file for a full-resolution image.
pub fn full_path(cache: &std::path::Path, id: &str) -> PathBuf {
    let dir = cache.join("full");
    std::fs::create_dir_all(&dir).ok();
    dir.join(format!("{id}.jpg"))
}

/// Persist the last successful search results so the app can show cached
/// content offline. Returns the file path.
pub fn last_search_path(cache: &std::path::Path) -> PathBuf {
    cache.join("last_search.json")
}

/// Save the last successful search results (metadata only — thumbnails are
/// already on disk in `thumbs/grid3`).
pub fn save_last_search(cache: &std::path::Path, wallpapers: &[crate::model::Wallpaper]) -> Result<()> {
    let path = last_search_path(cache);
    let json = serde_json::to_string(wallpapers)?;
    std::fs::write(&path, json)?;
    Ok(())
}

/// Load the last successful search results, if any.
pub fn load_last_search(cache: &std::path::Path) -> Result<Vec<crate::model::Wallpaper>> {
    let path = last_search_path(cache);
    let json = std::fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&json)?)
}

/// Favorites file: full wallpaper metadata so the Favorites view can render
/// the grid without a network round-trip.
pub fn favorites_path(cache: &std::path::Path) -> PathBuf {
    cache.join("favorites.json")
}

pub fn load_favorites(cache: &std::path::Path) -> Result<Vec<crate::model::Wallpaper>> {
    let path = favorites_path(cache);
    let json = std::fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&json)?)
}

pub fn save_favorites(
    cache: &std::path::Path,
    wallpapers: &[crate::model::Wallpaper],
) -> Result<()> {
    let path = favorites_path(cache);
    let json = serde_json::to_string(wallpapers)?;
    std::fs::write(&path, json)?;
    Ok(())
}

/// Wallpaper history: absolute paths of the last applied wallpapers (newest
/// first), capped at 10 for the Undo feature.
pub fn history_path(cache: &std::path::Path) -> PathBuf {
    cache.join("history.json")
}

pub fn load_history(cache: &std::path::Path) -> Result<Vec<String>> {
    let path = history_path(cache);
    let json = std::fs::read_to_string(&path)?;
    Ok(serde_json::from_str(&json)?)
}

pub fn save_history(cache: &std::path::Path, history: &[String]) -> Result<()> {
    let path = history_path(cache);
    let json = serde_json::to_string(history)?;
    std::fs::write(&path, json)?;
    Ok(())
}

/// User preferences persisted across launches.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    /// "dark" | "light" | "system".
    pub theme: String,
    /// Index into `WallpaperStyle::ALL`.
    pub wallpaper_style: u32,
    /// Seconds between slideshow steps.
    pub slideshow_interval_secs: u64,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            theme: "dark".into(),
            wallpaper_style: 0, // Fill
            slideshow_interval_secs: 30,
        }
    }
}

pub fn settings_path(cache: &std::path::Path) -> PathBuf {
    cache.join("settings.json")
}

pub fn load_settings(cache: &std::path::Path) -> Settings {
    let path = settings_path(cache);
    std::fs::read_to_string(&path)
        .ok()
        .and_then(|json| serde_json::from_str(&json).ok())
        .unwrap_or_default()
}

pub fn save_settings(cache: &std::path::Path, settings: &Settings) -> Result<()> {
    let path = settings_path(cache);
    let json = serde_json::to_string(settings)?;
    std::fs::write(&path, json)?;
    Ok(())
}

/// Cache size cap. When the cache exceeds this, the oldest files are deleted
/// (LRU by modified time) until it fits again.
const CACHE_LIMIT_BYTES: u64 = 500 * 1024 * 1024; // 500 MB

/// Enforce the cache size cap: if `cache/` holds more than 500 MB, delete the
/// oldest files (by last-modified time) until it's back under the limit.
/// Best-effort — never fails startup.
pub fn evict_if_needed(cache: &std::path::Path) {
    let Ok(entries) = collect_files(cache) else {
        return;
    };
    let total: u64 = entries.iter().map(|(_, size)| *size).sum();
    if total <= CACHE_LIMIT_BYTES {
        return;
    }
    tracing::info!(
        "cache {:.1} MB over limit; evicting oldest files",
        total as f64 / (1024.0 * 1024.0)
    );
    let mut over = total - CACHE_LIMIT_BYTES;
    for (path, size) in entries {
        if over == 0 {
            break;
        }
        if std::fs::remove_file(&path).is_ok() {
            over = over.saturating_sub(size);
        }
    }
}

/// All files under `cache/` with their sizes, oldest-modified first.
fn collect_files(cache: &std::path::Path) -> Result<Vec<(PathBuf, u64)>> {
    let mut files = Vec::new();
    for entry in std::fs::read_dir(cache)? {
        let entry = entry?;
        let path = entry.path();
        if path.is_dir() {
            files.extend(collect_files(&path)?);
        } else if let Ok(meta) = entry.metadata()
            && meta.is_file()
        {
            files.push((path, meta.len()));
        }
    }
    // Oldest first so eviction removes least-recently-used files.
    files.sort_by_key(|(path, _)| modified_ms(path));
    Ok(files)
}

/// Last-modified time in ms (0 if unavailable) — used as the LRU key.
fn modified_ms(path: &std::path::Path) -> u128 {
    std::fs::metadata(path)
        .and_then(|m| m.modified())
        .ok()
        .and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok())
        .map(|d| d.as_millis())
        .unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::{collect_files, evict_if_needed, CACHE_LIMIT_BYTES};

    fn test_dir(name: &str) -> std::path::PathBuf {
        let dir = std::env::temp_dir().join(format!("wallkraft-storage-{name}-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        dir
    }

    #[test]
    fn collect_files_walks_subdirs() {
        let dir = test_dir("collect");
        std::fs::create_dir_all(dir.join("thumbs/grid3")).unwrap();
        std::fs::create_dir_all(dir.join("full")).unwrap();
        std::fs::write(dir.join("thumbs/grid3/a.jpg"), vec![1u8; 10]).unwrap();
        std::fs::write(dir.join("full/b.jpg"), vec![2u8; 20]).unwrap();
        let files = collect_files(&dir).unwrap();
        assert_eq!(files.len(), 2);
        let total: u64 = files.iter().map(|(_, s)| *s).sum();
        assert_eq!(total, 30);
        std::fs::remove_dir_all(&dir).ok();
    }

    #[test]
    fn eviction_removes_oldest_when_over_limit() {
        let dir = test_dir("evict");
        // Two files: one old, one new. Total exceeds the cap.
        let old = dir.join("old.jpg");
        let new = dir.join("new.jpg");
        std::fs::write(&old, vec![0u8; (CACHE_LIMIT_BYTES / 2 + 1) as usize]).unwrap();
        std::fs::write(&new, vec![0u8; (CACHE_LIMIT_BYTES / 2 + 1) as usize]).unwrap();
        // Make `old` clearly older.
        let past = std::time::SystemTime::now() - std::time::Duration::from_secs(3600);
        let _ = filetime::set_file_mtime(&old, past.into());

        evict_if_needed(&dir);
        assert!(!old.exists(), "oldest file should be evicted");
        assert!(new.exists(), "newest file should survive");
        std::fs::remove_dir_all(&dir).ok();
    }

    #[test]
    fn eviction_skips_when_under_limit() {
        let dir = test_dir("under");
        std::fs::write(dir.join("a.jpg"), vec![0u8; 1024]).unwrap();
        evict_if_needed(&dir);
        assert!(dir.join("a.jpg").exists());
        std::fs::remove_dir_all(&dir).ok();
    }
}