//! Desktop wallpaper setter via the Win32 SystemParametersInfoW API.

use anyhow::{Context, Result};
use std::path::Path;
use windows::Win32::UI::WindowsAndMessaging::{
    SystemParametersInfoW, SPI_SETDESKWALLPAPER, SPIF_SENDCHANGE, SPIF_UPDATEINIFILE,
};

/// Set the desktop wallpaper to the image at `path`.
///
/// Windows 8+ accepts JPEG directly, so the downloaded full-res file can be
/// passed as-is. The path must be absolute.
pub fn set_wallpaper(path: &Path) -> Result<()> {
    let absolute = std::fs::canonicalize(path).context("wallpaper file not found")?;
    let wide: Vec<u16> = absolute
        .to_str()
        .context("path is not valid UTF-16")?
        .encode_utf16()
        .chain(std::iter::once(0))
        .collect();

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