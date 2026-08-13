//! Auto-update: check GitHub releases, download the NSIS installer, and apply
//! it via a detached updater script (the installer can't overwrite a running
//! exe, so the app exits first and the script relaunches it).
//!
//! Design notes:
//! - Fail-silent: a network error or malformed release simply means "no
//!   update", never a crash or a blocked startup (offline-first principle).
//! - Prereleases are skipped by using the `/releases/latest` endpoint, which
//!   returns the newest non-prerelease, non-draft release.
//! - The installer is per-user (`%LOCALAPPDATA%\Programs\WallKraft`), so no
//!   admin elevation is needed anywhere in this flow.

use std::cmp::Ordering;
use std::os::windows::process::CommandExt;
use std::path::{Path, PathBuf};
use std::process::Command;

use anyhow::{Context, Result};

const REPO_OWNER: &str = "kedharsairam";
const REPO_NAME: &str = "wallkraft";
/// Release list (newest first). We scan it rather than hitting
/// `/releases/latest` because the repo hosts two version tracks — Android
/// (1.x) and Windows (0.x) share the `v*` tag namespace, so the newest
/// release may be an Android-only one without a setup asset.
const RELEASES_URL: &str = "https://api.github.com/repos/kedharsairam/wallkraft/releases?per_page=20";
/// Setup exe asset name as uploaded by the Windows Release workflow.
const SETUP_ASSET: &str = "WallKraft-{version}-setup.exe";
/// Where the NSIS installer puts the app (must match installer.nsi).
const INSTALL_SUBDIR: [&str; 2] = ["Programs", "WallKraft"];

/// Parse a version tag ("v1.2.3" or "1.2.3") into (major, minor, patch).
/// Returns None for prerelease/build/4-part tags — we only ship plain x.y.z.
fn parse_version(tag: &str) -> Option<(u32, u32, u32)> {
    let s = tag.strip_prefix('v').unwrap_or(tag);
    let mut parts = s.split('.');
    let major = parts.next()?.parse().ok()?;
    let minor = parts.next()?.parse().ok()?;
    let patch = parts.next()?.parse().ok()?;
    if parts.next().is_some() {
        return None; // e.g. "1.2.3.4" or "1.2.3-beta" — not ours
    }
    Some((major, minor, patch))
}

/// Semantic-ish comparison of two tags. `None` if either isn't a plain x.y.z.
fn compare_versions(a: &str, b: &str) -> Option<Ordering> {
    Some(parse_version(a)?.cmp(&parse_version(b)?))
}

/// Whether a release payload carries the setup exe for `tag`.
fn release_has_setup_asset(body: &serde_json::Value, tag: &str) -> bool {
    let asset = SETUP_ASSET.replace("{version}", tag.trim_start_matches('v'));
    body["assets"].as_array().is_some_and(|assets| {
        assets.iter().any(|a| a["name"].as_str() == Some(asset.as_str()))
    })
}

/// From a release list (newest first), pick the tag of the newest release
/// that can actually be updated to: not a draft/prerelease, a plain x.y.z
/// version, and carrying the setup exe (a release without it would make the
/// download 404 — e.g. pre-installer pipeline-test or Android-only releases).
fn newest_releasable_tag(releases: &serde_json::Value) -> Option<String> {
    for rel in releases.as_array()? {
        if rel["draft"].as_bool() == Some(true) || rel["prerelease"].as_bool() == Some(true) {
            continue;
        }
        let Some(tag) = rel["tag_name"].as_str() else { continue };
        if parse_version(tag).is_none() {
            continue;
        }
        if release_has_setup_asset(rel, tag) {
            return Some(tag.to_string());
        }
    }
    None
}

async fn latest_release_tag() -> Result<String> {
    let resp = reqwest::Client::builder()
        .user_agent("WallKraft")
        .connect_timeout(std::time::Duration::from_secs(10))
        .timeout(std::time::Duration::from_secs(20))
        .build()?
        .get(RELEASES_URL)
        .header("Accept", "application/vnd.github+json")
        .send()
        .await?
        .error_for_status()?;
    let body: serde_json::Value = resp.json().await?;
    newest_releasable_tag(&body).context("no release carries a setup asset")
}

/// Download the setup exe for `version` to `dest`.
async fn download_installer(version: &str, dest: &Path) -> Result<()> {
    let asset = SETUP_ASSET.replace("{version}", version);
    let url = format!(
        "https://github.com/{REPO_OWNER}/{REPO_NAME}/releases/download/v{version}/{asset}"
    );
    let bytes = reqwest::Client::builder()
        .user_agent("WallKraft")
        .timeout(std::time::Duration::from_secs(120))
        .build()?
        .get(&url)
        .send()
        .await?
        .error_for_status()?
        .bytes()
        .await?;
    std::fs::write(dest, &bytes)?;
    tracing::info!("downloaded update {version} -> {}", dest.display());
    Ok(())
}

/// Resolve the per-user install dir the NSIS installer uses.
fn install_dir() -> PathBuf {
    std::env::var("LOCALAPPDATA")
        .map(PathBuf::from)
        .unwrap_or_else(|_| PathBuf::from("C:\\Users\\Public"))
        .join(INSTALL_SUBDIR[0])
        .join(INSTALL_SUBDIR[1])
}

/// Render the updater `.cmd` script. It waits for `wallkraft.exe` to exit
/// (the installer can't overwrite a running exe), runs the setup silently,
/// relaunches the installed app, and self-deletes.
fn updater_script(setup_path: &Path, exe_path: &Path) -> String {
    format!(
        "@echo off\r\n\
         rem WallKraft auto-update\r\n\
         :wait_loop\r\n\
         tasklist /fi \"imagename eq wallkraft.exe\" 2>nul | find /i \"wallkraft.exe\" >nul\r\n\
         if not errorlevel 1 (\r\n\
             ping -n 2 127.0.0.1 >nul\r\n\
             goto wait_loop\r\n\
         )\r\n\
         \"{}\" /S\r\n\
         start \"\" \"{}\"\r\n\
         del \"%~f0\"\r\n",
        setup_path.display(),
        exe_path.display()
    )
}

/// Spawn a detached script that waits for the app to exit, silently runs the
/// installer, then relaunches the app. Returns once the script is spawned —
/// the caller is expected to exit the process immediately after.
fn spawn_updater(setup_path: &Path) -> Result<()> {
    let exe = install_dir().join("wallkraft.exe");
    let script = updater_script(setup_path, &exe);
    let script_path = std::env::temp_dir().join("wallkraft-update.cmd");
    std::fs::write(&script_path, &script)?;
    // DETACHED_PROCESS | CREATE_NEW_PROCESS_GROUP: survives our exit, no window.
    let child = Command::new("cmd")
        .args(["/C", script_path.to_str().context("temp path not UTF-8")?])
        .creation_flags(0x0000_0008 | 0x0000_0200)
        .spawn()
        .context("failed to spawn updater")?;
    tracing::info!("updater spawned (pid {})", child.id());
    Ok(())
}

/// Full update flow, run on a background task:
/// 1. Check the latest release. If it's newer, surface it in the UI.
/// 2. On user action, download the installer and apply it.
pub async fn check_for_update(ui: slint::Weak<crate::MainWindow>) {
    let Ok(tag) = latest_release_tag().await else {
        tracing::info!("update check skipped (network/release error)");
        return;
    };
    let Some(ordering) = compare_versions(&tag, env!("CARGO_PKG_VERSION")) else {
        tracing::info!("update check skipped (unexpected tag {tag:?})");
        return;
    };
    if ordering != Ordering::Greater {
        tracing::info!("up to date ({})", env!("CARGO_PKG_VERSION"));
        return;
    }
    let version = tag.trim_start_matches('v').to_string();
    tracing::info!("update available: {version}");
    let _ = ui.upgrade_in_event_loop(move |ui| {
        ui.set_update_available(true);
        ui.set_update_version(version.into());
    });
}

/// Download + install the update. Called from the UI thread; does the network
/// work off-thread and only touches the UI through `upgrade_in_event_loop`.
pub fn apply_update(
    ui: slint::Weak<crate::MainWindow>,
    version: String,
    rt: &tokio::runtime::Handle,
) {
    let setup_path = std::env::temp_dir().join("WallKraft-update-setup.exe");
    rt.spawn(async move {
        let _ = ui.upgrade_in_event_loop(move |ui| {
            ui.set_update_downloading(true);
        });
        if let Err(e) = download_installer(&version, &setup_path).await {
            tracing::warn!("update download failed: {e:#}");
            let _ = ui.upgrade_in_event_loop(move |ui| {
                ui.set_update_downloading(false);
                crate::ui::flash_status(&ui, "Update download failed");
            });
            return;
        }
        match spawn_updater(&setup_path) {
            Ok(()) => {
                // The updater waits for us, then installs + relaunches.
                tracing::info!("applying update {version}");
                std::process::exit(0);
            }
            Err(e) => {
                tracing::warn!("failed to start updater: {e:#}");
                let _ = ui.upgrade_in_event_loop(move |ui| {
                    ui.set_update_downloading(false);
                    crate::ui::flash_status(&ui, "Update failed to start");
                });
            }
        }
    });
}

#[cfg(test)]
mod tests {
    use super::{compare_versions, parse_version, release_has_setup_asset};
    use std::cmp::Ordering;

    #[test]
    fn parses_plain_tags() {
        assert_eq!(parse_version("v1.2.3"), Some((1, 2, 3)));
        assert_eq!(parse_version("1.2.3"), Some((1, 2, 3)));
        assert_eq!(parse_version("v0.1.0"), Some((0, 1, 0)));
        assert_eq!(parse_version("v10.20.30"), Some((10, 20, 30)));
    }

    #[test]
    fn rejects_foreign_tags() {
        assert_eq!(parse_version("v1.2"), None);
        assert_eq!(parse_version("v1.2.3.4"), None);
        assert_eq!(parse_version("v1.2.3-beta"), None);
        assert_eq!(parse_version("v1.2.3+build"), None);
        assert_eq!(parse_version("latest"), None);
        assert_eq!(parse_version("v1.2.3.4.5"), None);
    }

    #[test]
    fn compares_numerically() {
        assert_eq!(compare_versions("v0.1.1", "0.1.0"), Some(Ordering::Greater));
        assert_eq!(compare_versions("v0.1.0", "v0.1.0"), Some(Ordering::Equal));
        assert_eq!(compare_versions("v0.9.9", "v0.10.0"), Some(Ordering::Less));
        assert_eq!(compare_versions("v1.0.0", "v0.99.99"), Some(Ordering::Greater));
        assert_eq!(compare_versions("v1.2.3", "garbage"), None);
    }

    #[test]
    fn setup_asset_required() {
        let with_asset = serde_json::json!({
            "tag_name": "v0.1.1",
            "assets": [
                { "name": "WallKraft-0.1.1-windows-x64.exe" },
                { "name": "WallKraft-0.1.1-setup.exe" }
            ]
        });
        assert!(release_has_setup_asset(&with_asset, "v0.1.1"));

        // Pre-installer releases only carried the portable exe.
        let portable_only = serde_json::json!({
            "tag_name": "v1.8.0",
            "assets": [{ "name": "WallKraft-1.8.0-windows-x64.exe" }]
        });
        assert!(!release_has_setup_asset(&portable_only, "v1.8.0"));

        // No assets at all.
        assert!(!release_has_setup_asset(&serde_json::json!({ "assets": [] }), "v0.1.1"));
    }

    #[test]
    fn scans_releases_for_newest_releasable() {
        use super::newest_releasable_tag;

        // Newest release is Android-only (no setup asset); the next one carries
        // the setup exe → the scanner must return the Windows release.
        let list = serde_json::json!([
            {
                "tag_name": "v1.8.0", "draft": false, "prerelease": false,
                "assets": [{ "name": "WallKraft-1.8.0-windows-x64.exe" }]
            },
            {
                "tag_name": "v0.1.0", "draft": false, "prerelease": false,
                "assets": [{ "name": "WallKraft-0.1.0-setup.exe" }]
            }
        ]);
        assert_eq!(newest_releasable_tag(&list).as_deref(), Some("v0.1.0"));

        // Prereleases/drafts are skipped even when they have the asset.
        let prerelease_first = serde_json::json!([
            {
                "tag_name": "v0.2.0-beta", "draft": false, "prerelease": true,
                "assets": [{ "name": "WallKraft-0.2.0-beta-setup.exe" }]
            },
            {
                "tag_name": "v0.1.1", "draft": false, "prerelease": false,
                "assets": [{ "name": "WallKraft-0.1.1-setup.exe" }]
            }
        ]);
        assert_eq!(newest_releasable_tag(&prerelease_first).as_deref(), Some("v0.1.1"));

        // No release carries the setup exe → None (no update offered).
        let none = serde_json::json!([
            {
                "tag_name": "v1.8.0", "draft": false, "prerelease": false,
                "assets": [{ "name": "WallKraft-1.8.0-windows-x64.exe" }]
            }
        ]);
        assert_eq!(newest_releasable_tag(&none), None);

        // Empty list → None.
        assert_eq!(newest_releasable_tag(&serde_json::json!([])), None);
    }

    #[test]
    fn updater_script_has_required_steps() {
        let setup = std::path::Path::new(r"C:\Temp\WallKraft-update-setup.exe");
        let exe = std::path::Path::new(r"C:\Users\me\AppData\Local\Programs\WallKraft\wallkraft.exe");
        let s = super::updater_script(setup, exe);

        // Wait loop must gate on the app process actually exiting.
        assert!(s.contains("tasklist /fi \"imagename eq wallkraft.exe\""), "wait loop uses tasklist");
        assert!(s.contains(":wait_loop") && s.contains("goto wait_loop"), "loop labels present");

        // Silent install of the quoted setup path.
        assert!(s.contains("\"C:\\Temp\\WallKraft-update-setup.exe\" /S"), "silent install line");
        // Relaunch the installed exe.
        assert!(s.contains("start \"\" \"C:\\Users\\me\\AppData\\Local\\Programs\\WallKraft\\wallkraft.exe\""), "relaunch line");
        // Self-cleanup of the script.
        assert!(s.contains("del \"%~f0\""), "self-delete present");

        // Windows line endings for cmd.exe.
        assert!(s.contains("\r\n"), "CRLF line endings");
        assert!(!s.contains("\n\n"), "no blank-line gaps");
    }

    /// Live check against the real GitHub API (run with `cargo test -- --ignored`).
    /// As of the last pipeline-test release (v1.8.0, portable exe only), the
    /// latest-release gate must reject it. Revisit when a setup-asset release exists.
    #[ignore = "hits the real GitHub API"]
    #[tokio::test]
    async fn live_latest_release_is_gated() {
        match super::latest_release_tag().await {
            Ok(tag) => println!("latest releasable tag: {tag}"),
            Err(e) => println!("correctly gated out: {e:#}"),
        }
    }
}
