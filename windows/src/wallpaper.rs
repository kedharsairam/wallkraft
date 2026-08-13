//! Desktop wallpaper setter via the Win32 SystemParametersInfoW API.
//!
//! The style (fill/fit/stretch/center/tile/span) is stored in the registry
//! (`HKCU\Control Panel\Desktop\WallpaperStyle` + `TileWallpaper`) before the
//! wallpaper is applied, mirroring what the Windows Settings app does.

use anyhow::{Context, Result};
use std::path::Path;
use windows::core::PCWSTR;
use windows::Win32::System::Registry::{
    RegCloseKey, RegOpenKeyExW, RegSetValueExW, HKEY, HKEY_CURRENT_USER, KEY_SET_VALUE, REG_DWORD,
};
use windows::Win32::UI::WindowsAndMessaging::{
    SystemParametersInfoW, SPI_SETDESKWALLPAPER, SPIF_SENDCHANGE, SPIF_UPDATEINIFILE,
};

/// How the wallpaper is positioned on the desktop.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WallpaperStyle {
    /// Centered, no scaling.
    Center,
    /// Scaled to fit (letterboxed).
    Fit,
    /// Scaled to fill (cropped).
    Fill,
    /// Stretched to the full screen (distorts aspect).
    Stretch,
    /// Tiled.
    Tile,
    /// Spanned across all monitors.
    Span,
}

impl WallpaperStyle {
    /// `WallpaperStyle` registry value (0 = center, 6 = fit, 10 = fill,
    /// 2 = stretch, 22 = span). Tile uses 0 with `TileWallpaper = 1`.
    fn registry_value(self) -> u32 {
        match self {
            WallpaperStyle::Center | WallpaperStyle::Tile => 0,
            WallpaperStyle::Fit => 6,
            WallpaperStyle::Fill => 10,
            WallpaperStyle::Stretch => 2,
            WallpaperStyle::Span => 22,
        }
    }

    fn tile_wallpaper(self) -> u32 {
        match self {
            WallpaperStyle::Tile => 1,
            _ => 0,
        }
    }

    /// All styles in dropdown order.
    pub const ALL: [WallpaperStyle; 6] = [
        WallpaperStyle::Fill,
        WallpaperStyle::Fit,
        WallpaperStyle::Stretch,
        WallpaperStyle::Center,
        WallpaperStyle::Tile,
        WallpaperStyle::Span,
    ];

    pub fn from_index(i: usize) -> Self {
        Self::ALL.get(i).copied().unwrap_or(WallpaperStyle::Fill)
    }
}

/// Set the desktop wallpaper to the image at `path` with the given style.
///
/// Windows 8+ accepts JPEG directly, so the downloaded full-res file can be
/// passed as-is. The path must be absolute.
pub fn set_wallpaper(path: &Path, style: WallpaperStyle) -> Result<()> {
    let absolute = std::fs::canonicalize(path).context("wallpaper file not found")?;
    let wide: Vec<u16> = absolute
        .to_str()
        .context("path is not valid UTF-16")?
        .encode_utf16()
        .chain(std::iter::once(0))
        .collect();

    set_desktop_registry(style)?;

    unsafe {
        SystemParametersInfoW(
            SPI_SETDESKWALLPAPER,
            0,
            Some(wide.as_ptr() as *mut core::ffi::c_void),
            SPIF_UPDATEINIFILE | SPIF_SENDCHANGE,
        )
    }
    .context("SystemParametersInfoW(SPI_SETDESKWALLPAPER) failed")
}

/// Write the style to `HKCU\Control Panel\Desktop` so the desktop honors it.
fn set_desktop_registry(style: WallpaperStyle) -> Result<()> {
    let desktop_wide: Vec<u16> = "Control Panel\\Desktop"
        .encode_utf16()
        .chain(std::iter::once(0))
        .collect();
    let mut key: HKEY = HKEY::default();
    unsafe {
        RegOpenKeyExW(
            HKEY_CURRENT_USER,
            PCWSTR::from_raw(desktop_wide.as_ptr()),
            Some(0),
            KEY_SET_VALUE,
            &mut key,
        )
        .ok()
        .context("RegOpenKeyExW(Control Panel\\Desktop) failed")?;
        let result = (|| -> Result<()> {
            set_dword(key, "WallpaperStyle", style.registry_value())?;
            set_dword(key, "TileWallpaper", style.tile_wallpaper())?;
            Ok(())
        })();
        let _ = RegCloseKey(key);
        result
    }
}

fn set_dword(key: HKEY, name: &str, value: u32) -> Result<()> {
    let name_wide: Vec<u16> = name.encode_utf16().chain(std::iter::once(0)).collect();
    let value_bytes = value.to_ne_bytes();
    unsafe {
        RegSetValueExW(
            key,
            PCWSTR::from_raw(name_wide.as_ptr()),
            Some(0),
            REG_DWORD,
            Some(&value_bytes),
        )
        .ok()
        .context(format!("RegSetValueExW({name}) failed"))
    }
}

#[cfg(test)]
mod tests {
    use super::WallpaperStyle;

    #[test]
    fn registry_values_match_windows_docs() {
        assert_eq!(WallpaperStyle::Center.registry_value(), 0);
        assert_eq!(WallpaperStyle::Fit.registry_value(), 6);
        assert_eq!(WallpaperStyle::Fill.registry_value(), 10);
        assert_eq!(WallpaperStyle::Stretch.registry_value(), 2);
        assert_eq!(WallpaperStyle::Span.registry_value(), 22);
        assert_eq!(WallpaperStyle::Tile.tile_wallpaper(), 1);
        assert_eq!(WallpaperStyle::Fill.tile_wallpaper(), 0);
    }

    #[test]
    fn from_index_roundtrips() {
        for (i, s) in WallpaperStyle::ALL.iter().enumerate() {
            assert_eq!(WallpaperStyle::from_index(i), *s);
        }
        assert_eq!(WallpaperStyle::from_index(99), WallpaperStyle::Fill);
    }
}