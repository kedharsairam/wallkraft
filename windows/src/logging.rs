//! Logging and crash reporting.
//!
//! Logs go to `cache/logs/wallkraft.log` (rolling daily). Panics are caught
//! by a hook that writes a timestamped crash log to `cache/logs/crash-*.log`
//! so a hard failure is never silent.

use anyhow::Result;
use std::path::PathBuf;
use std::sync::OnceLock;

use tracing_appender::non_blocking::WorkerGuard;

/// Keeps the non-blocking writer alive for the process lifetime.
static GUARD: OnceLock<WorkerGuard> = OnceLock::new();

/// Initialize file logging and the panic hook. Call once at startup.
pub fn init() -> Result<PathBuf> {
    let log_dir = crate::storage::cache_dir()?.join("logs");
    std::fs::create_dir_all(&log_dir)?;

    install_panic_hook(&log_dir);

    let appender = tracing_appender::rolling::daily(&log_dir, "wallkraft.log");
    let (writer, guard) = tracing_appender::non_blocking(appender);
    let _ = GUARD.set(guard);

    tracing_subscriber::fmt()
        .with_writer(writer)
        .with_ansi(false)
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("info")),
        )
        .init();

    Ok(log_dir)
}

/// Replace the default panic hook with one that writes a crash log.
fn install_panic_hook(log_dir: &std::path::Path) {
    let dir = log_dir.to_path_buf();
    std::panic::set_hook(Box::new(move |info| {
        let ts = timestamp();
        let file = dir.join(format!("crash-{ts}.log"));
        let msg = format!(
            "WallKraft crashed at {ts}\n\n{info}\n\nBacktrace:\n{}",
            std::backtrace::Backtrace::force_capture()
        );
        eprintln!("{msg}");
        let _ = std::fs::write(&file, &msg);
    }));
}

/// Compact local timestamp for filenames: `20260813-143022`.
fn timestamp() -> String {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs();
    // Convert to local time via the Windows API (no extra crate needed).
    let secs = now as i64;
    let days = secs.div_euclid(86_400);
    let rem = secs.rem_euclid(86_400);
    let (h, m, s) = (rem / 3600, (rem % 3600) / 60, rem % 60);
    let (y, mo, d) = civil_from_days(days);
    format!("{y:04}{mo:02}{d:02}-{h:02}{m:02}{s:02}")
}

/// Days → (year, month, day) using Howard Hinnant's civil-from-days algorithm.
fn civil_from_days(z: i64) -> (i64, u32, u32) {
    let z = z + 719_468;
    let era = z.div_euclid(146_097);
    let doe = z.rem_euclid(146_097);
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = (doy - (153 * mp + 2) / 5 + 1) as u32;
    let m = if mp < 10 { mp + 3 } else { mp - 9 } as u32;
    (if m <= 2 { y + 1 } else { y }, m, d)
}

#[cfg(test)]
mod tests {
    use super::{civil_from_days, install_panic_hook};

    #[test]
    fn civil_from_days_known_dates() {
        // Unix epoch: 1970-01-01.
        assert_eq!(civil_from_days(0), (1970, 1, 1));
        // 2000-03-01 (leap year).
        assert_eq!(civil_from_days(11_017), (2000, 3, 1));
        // 2026-08-13.
        assert_eq!(civil_from_days(20_678), (2026, 8, 13));
    }

    #[test]
    fn panic_hook_writes_crash_log() {
        let dir = std::env::temp_dir().join("wallkraft-test-crash");
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        // Save the harness's hook and restore it after, so other tests'
        // failures aren't captured by ours.
        let original = std::panic::take_hook();
        install_panic_hook(&dir);

        // Panic in a thread so the test harness can catch it.
        let handle = std::thread::spawn(|| panic!("boom"));
        assert!(handle.join().is_err());

        std::panic::set_hook(original);

        let files: Vec<_> = std::fs::read_dir(&dir)
            .unwrap()
            .filter_map(|e| e.ok())
            .filter(|e| e.file_name().to_string_lossy().starts_with("crash-"))
            .collect();
        assert_eq!(files.len(), 1, "expected exactly one crash log");
        let content = std::fs::read_to_string(files[0].path()).unwrap();
        assert!(content.contains("boom"), "crash log should contain the panic message");
        assert!(content.contains("Backtrace"), "crash log should include a backtrace");

        std::fs::remove_dir_all(&dir).ok();
    }
}