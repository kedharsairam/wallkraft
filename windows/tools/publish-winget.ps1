<#
.SYNOPSIS
    Generate winget manifests for WallKraft and validate them.

.DESCRIPTION
    Produces the three multi-file winget manifests (1.12.0 schema) for a given
    version, computes the SHA256 of the NSIS setup exe, and runs `winget
    validate` against the result. The output folder is laid out exactly like a
    winget-pkgs checkout (manifests/k/KedharSairam/WallKraft/<version>/), so it
    can be copied straight into a winget-pkgs fork for a submission PR.

    Only run this AFTER the release has been pushed and the setup exe is
    attached as a release asset — the InstallerUrl must resolve for winget's
    URL validation.

.PARAMETER Version
    Semver to publish, e.g. "0.1.1". Defaults to the version in Cargo.toml.

.PARAMETER SetupPath
    Path to the setup exe. Defaults to target/release/WallKraft-<Version>-setup.exe.

.PARAMETER OutDir
    Where to write the manifest folder. Defaults to ..\winget-out.

.EXAMPLE
    .\tools\publish-winget.ps1 -Version 0.1.1
#>
[CmdletBinding()]
param(
    [string]$Version,
    [string]$SetupPath,
    [string]$OutDir,
    [switch]$SkipUrlCheck
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot   # ...\wallkraft\windows
$pkgRoot  = Split-Path -Parent $repoRoot       # ...\wallkraft

if (-not $Version) {
    $versionLine = Get-Content (Join-Path $repoRoot "Cargo.toml") | Where-Object { $_ -match '^version\s*=' } | Select-Object -First 1
    $Version = ($versionLine -split '"')[1]
}
if (-not $SetupPath) {
    $SetupPath = Join-Path $repoRoot "target\release\WallKraft-$Version-setup.exe"
}
if (-not $OutDir) {
    $OutDir = Join-Path $pkgRoot "winget-out"
}
if (-not (Test-Path $SetupPath)) {
    throw "Setup exe not found at $SetupPath"
}

$hash = (Get-FileHash $SetupPath -Algorithm SHA256).Hash
$sizeMB = [math]::Round((Get-Item $SetupPath).Length / 1MB, 2)
Write-Host "PackageVersion : $Version"
Write-Host "Setup exe      : $SetupPath ($sizeMB MB)"
Write-Host "SHA256         : $hash"

# The hash in the manifest must match the bytes served from the release asset
# EXACTLY. CI builds (different makensis/Rust toolchain than this machine) can
# produce different bytes, so verify against the live URL when the release
# exists. Skippable with -SkipUrlCheck (never recommend this for real PRs).
$releaseUrl = "https://github.com/kedharsairam/wallkraft/releases/download/v$Version/WallKraft-$Version-setup.exe"
if (-not $SkipUrlCheck) {
    Write-Host ""
    Write-Host "Checking released asset against the local hash..."
    $tmpRemote = Join-Path $env:TEMP "WallKraft-$Version-urlcheck.exe"
    # curl writes errors to stderr, which PS 5.1 surfaces as NativeCommandError;
    # tolerate that here and branch on $LASTEXITCODE instead.
    $oldPref = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    & curl.exe -L -sS --fail --output $tmpRemote $releaseUrl 2>$null
    $ErrorActionPreference = $oldPref
    if ($LASTEXITCODE -eq 0) {
        $remoteHash = (Get-FileHash $tmpRemote -Algorithm SHA256).Hash
        if ($remoteHash -eq $hash) {
            Write-Host "Released asset SHA256 matches local build." -ForegroundColor Green
        } else {
            Remove-Item $tmpRemote -Force
            throw "Hash mismatch! Released asset: $remoteHash, local: $hash. The CI build differs from this machine - re-run this script after the release so the hash matches the ACTUAL asset."
        }
    } else {
        Write-Host "Release asset not reachable (exit $LASTEXITCODE) - release not pushed yet?" -ForegroundColor Yellow
        Write-Host "Hash was NOT verified against the live URL. Re-run this script AFTER pushing the release." -ForegroundColor Yellow
    }
    Remove-Item $tmpRemote -Force -ErrorAction SilentlyContinue
}

$manifestDir = Join-Path $OutDir "manifests\k\KedharSairam\WallKraft\$Version"
New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null

# --- version manifest ---
@"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.version.1.12.0.schema.json
PackageIdentifier: KedharSairam.WallKraft
PackageVersion: $Version
DefaultLocale: en-US
ManifestType: version
ManifestVersion: 1.12.0
"@ | Set-Content -Path (Join-Path $manifestDir "KedharSairam.WallKraft.yaml") -Encoding UTF8

# --- default locale manifest ---
@"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.defaultLocale.1.12.0.schema.json
PackageIdentifier: KedharSairam.WallKraft
PackageVersion: $Version
PackageLocale: en-US
Publisher: Kedhar Sairam
PublisherUrl: https://github.com/kedharsairam
Author: Kedhar Sairam
PackageName: WallKraft
PackageUrl: https://github.com/kedharsairam/wallkraft
License: MIT
LicenseUrl: https://github.com/kedharsairam/wallkraft/blob/master/LICENSE
ShortDescription: Browse and apply wallpapers from Wallhaven
Description: WallKraft is a desktop wallpaper browser for Wallhaven with a masonry grid, search and filters, favorites, slideshow, a system tray, drag-and-drop saving, and a per-user NSIS installer. Written in Rust with the Slint UI toolkit.
Moniker: wallkraft
Tags:
- wallpaper
- wallhaven
- desktop
- rust
- slint
ReleaseNotesUrl: https://github.com/kedharsairam/wallkraft/releases
ManifestType: defaultLocale
ManifestVersion: 1.12.0
"@ | Set-Content -Path (Join-Path $manifestDir "KedharSairam.WallKraft.locale.en-US.yaml") -Encoding UTF8

# --- installer manifest ---
@"
# yaml-language-server: `$schema=https://aka.ms/winget-manifest.installer.1.12.0.schema.json
PackageIdentifier: KedharSairam.WallKraft
PackageVersion: $Version
InstallerType: nullsoft
Scope: user
InstallModes:
- silent
- silentWithProgress
InstallerSwitches:
  Silent: /S
  SilentWithProgress: /S
UpgradeBehavior: install
# The app self-updates via its built-in updater, so winget shouldn't nag on
# `winget upgrade --all`; explicit upgrades still work.
RequireExplicitUpgrade: true
Installers:
- Architecture: x64
  InstallerUrl: https://github.com/kedharsairam/wallkraft/releases/download/v$Version/WallKraft-$Version-setup.exe
  InstallerSha256: $hash
  AppsAndFeaturesEntries:
  - DisplayName: WallKraft
    Publisher: Kedhar Sairam
    ProductCode: WallKraft
ManifestType: installer
ManifestVersion: 1.12.0
"@ | Set-Content -Path (Join-Path $manifestDir "KedharSairam.WallKraft.installer.yaml") -Encoding UTF8

Write-Host ""
Write-Host "Manifests written to: $manifestDir"
Get-ChildItem $manifestDir | ForEach-Object { Write-Host "  - $($_.Name)" }

# --- validate locally (winget ships with Windows 11) ---
$winget = Get-Command winget -ErrorAction SilentlyContinue
if ($winget) {
    Write-Host ""
    Write-Host "Validating with winget (this requires network access)..."
    & winget validate $manifestDir
    if ($LASTEXITCODE -ne 0) {
        Write-Host "VALIDATION FAILED (exit $LASTEXITCODE)" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "Validation passed." -ForegroundColor Green
}

Write-Host ""
Write-Host "NEXT STEPS - submit to microsoft/winget-pkgs:"
Write-Host "  1. Fork github.com/microsoft/winget-pkgs and clone it"
Write-Host "  2. Copy the manifests folder:"
Write-Host "       Copy-Item -Recurse '$(Join-Path $OutDir 'manifests')' '<winget-pkgs clone>\manifests'"
Write-Host "  3. Commit, push, and open a PR titled 'New version: KedharSairam.WallKraft version $Version'"
Write-Host "     The winget-pkgs validation bot checks schema, hash, and installer URL."
