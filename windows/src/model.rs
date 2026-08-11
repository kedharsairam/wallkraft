//! Data models mirroring the Wallhaven API response shape.

use serde::Deserialize;

#[derive(Debug, Clone, Deserialize)]
pub struct Wallpaper {
    pub id: String,
    pub url: String,
    /// Full-resolution image URL.
    pub path: String,
    pub resolution: String,
    pub category: String,
    pub purity: String,
    #[serde(rename = "dimension_x")]
    pub dimension_x: u32,
    #[serde(rename = "dimension_y")]
    pub dimension_y: u32,
    pub thumbs: Thumbs,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Thumbs {
    pub large: String,
    pub original: String,
    pub small: String,
}

#[derive(Debug, Deserialize)]
pub struct SearchResponse {
    pub data: Vec<Wallpaper>,
}