//! Image processing helpers (downscaling, decoding).

/// Downscale a JPEG file in place so its largest side is <= 384px, preserving
/// aspect ratio. Returns None if the file couldn't be read or written.
pub fn downscale_jpeg(path: &std::path::Path) -> Option<()> {
    let img = image::open(path).ok()?;
    let (w, h) = (img.width(), img.height());
    let max = w.max(h);
    if max > 384 {
        let scale = 384.0 / max as f32;
        let nw = ((w as f32) * scale).round().max(1.0) as u32;
        let nh = ((h as f32) * scale).round().max(1.0) as u32;
        img.resize(nw, nh, image::imageops::FilterType::Triangle)
            .save(path)
            .ok()?;
    }
    Some(())
}

#[cfg(test)]
mod tests {
    use super::downscale_jpeg;

    fn write_jpeg(path: &std::path::Path, w: u32, h: u32) {
        let buf = vec![128u8; (w * h * 3) as usize];
        image::save_buffer(path, &buf, w, h, image::ExtendedColorType::Rgb8).unwrap();
    }

    fn dims(path: &std::path::Path) -> (u32, u32) {
        let img = image::open(path).unwrap();
        (img.width(), img.height())
    }

    #[test]
    fn large_image_is_downscaled_to_384() {
        let dir = std::env::temp_dir().join("wallkraft-test-images");
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("large.jpg");
        write_jpeg(&path, 1920, 1080);
        assert!(downscale_jpeg(&path).is_some());
        let (w, h) = dims(&path);
        assert_eq!(w.max(h), 384, "largest side must be capped at 384");
        // Aspect ratio preserved: 1920x1080 -> 384x216.
        assert_eq!(w, 384);
        assert_eq!(h, 216);
    }

    #[test]
    fn small_image_is_left_untouched() {
        let dir = std::env::temp_dir().join("wallkraft-test-images");
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("small.jpg");
        write_jpeg(&path, 200, 100);
        assert!(downscale_jpeg(&path).is_some());
        assert_eq!(dims(&path), (200, 100));
    }

    #[test]
    fn portrait_image_preserves_orientation() {
        let dir = std::env::temp_dir().join("wallkraft-test-images");
        std::fs::create_dir_all(&dir).unwrap();
        let path = dir.join("portrait.jpg");
        write_jpeg(&path, 1080, 1920);
        assert!(downscale_jpeg(&path).is_some());
        let (w, h) = dims(&path);
        assert_eq!(w.max(h), 384);
        assert_eq!(w, 216);
        assert_eq!(h, 384);
    }
}