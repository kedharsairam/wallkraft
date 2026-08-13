fn main() {
    slint_build::compile("ui/main.slint").expect("failed to compile Slint UI");

    // Embed the app icon, Windows manifest (DPI awareness, common controls),
    // and version info into the exe. The version numbers come from
    // Cargo.toml automatically (CARGO_PKG_VERSION) — see [package.metadata.winres].
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("windows") {
        let mut res = winres::WindowsResource::new();
        res.set_icon("assets/wallkraft.ico");
        res.set_manifest_file("assets/wallkraft.manifest");
        res.set_language(0x0409); // en-US
        res.set("OriginalFilename", "wallkraft.exe");
        if let Err(e) = res.compile() {
            eprintln!("winres: {e}");
            std::process::exit(1);
        }
    }
}
