//! Local storage: cache directory for downloaded images.

use anyhow::{Context, Result};
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

/// Cache file for a thumbnail (small preview).
pub fn thumb_path(cache: &PathBuf, id: &str) -> PathBuf {
    let dir = cache.join("thumbs");
    std::fs::create_dir_all(&dir).ok();
    dir.join(format!("{id}.jpg"))
}

/// Cache file for a full-resolution image.
pub fn full_path(cache: &PathBuf, id: &str) -> PathBuf {
    let dir = cache.join("full");
    std::fs::create_dir_all(&dir).ok();
    dir.join(format!("{id}.jpg"))
}