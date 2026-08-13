//! Data models mirroring the Wallhaven API response shape.
//!
//! New fields added over time carry `#[serde(default)]` so JSON cached by
//! older builds still parses.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct Wallpaper {
    pub id: String,
    /// Wallhaven page URL (used by the detail view's "Open on wallhaven.cc").
    pub url: String,
    /// Full-resolution image URL.
    pub path: String,
    pub resolution: String,
    #[serde(rename = "dimension_x")]
    pub dimension_x: u32,
    #[serde(rename = "dimension_y")]
    pub dimension_y: u32,
    pub thumbs: Thumbs,
    #[serde(default)]
    pub views: u64,
    #[serde(default)]
    pub favorites: u64,
    /// "sfw" | "sketchy" | "nsfw".
    #[serde(default)]
    pub purity: String,
    /// "general" | "anime" | "people".
    #[serde(default)]
    pub category: String,
    /// Dominant colors as "#rrggbb".
    #[serde(default)]
    pub colors: Vec<String>,
    #[serde(default)]
    pub tags: Vec<Tag>,
    #[serde(default)]
    pub user: Option<User>,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct Thumbs {
    pub large: String,
    pub original: String,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct Tag {
    pub id: u64,
    pub name: String,
    pub namespace: String,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct User {
    pub username: String,
}

#[derive(Debug, Deserialize)]
pub struct SearchResponse {
    pub data: Vec<Wallpaper>,
}
