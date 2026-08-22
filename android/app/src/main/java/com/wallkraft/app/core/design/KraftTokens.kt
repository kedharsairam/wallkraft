package com.wallkraft.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WallKraft Aurora — Design tokens for the WallKraft wallpaper browser.
 *
 * Palette concept: Neutral Canvas + Aurora Accent.
 * Wallpapers are the hero; UI is a desaturated deep-space canvas with one
 * Aurora Blue accent for all interactive elements. Every color is tuned for
 * 4.5:1 contrast in both light and dark modes.
 *
 * Follows the Kraft Design Language from kraft-standards.
 */
object KraftColors {
    // ─── Aurora Accent (one hue, two weights) ───────────────────────────
    val AccentBlue = Color(0xFF0066FF)       // Light primary — buttons, chips, active nav
    val AccentBlueDark = Color(0xFF4D8AFF)   // Dark primary — 40% lighter for 4.5:1 on #0A0E1A
    val AccentGreen = Color(0xFF00C896)      // Download badge — teal-green, matches Aurora
    val AccentGreenDark = Color(0xFF00E6A8)  // Dark variant
    val AccentRed = Color(0xFFFF3B30)        // Heart favorite — Apple red, universal
    val AccentRedDark = Color(0xFFFF453A)
    val AccentOrange = Color(0xFFFF9500)     // Warning accent
    val AccentOrangeDark = Color(0xFFFF9F0A)

    // ─── Backgrounds & Surfaces — Dawn (Light) ──────────────────────────
    // Warm paper, not pure white — easy on eyes with colorful wallpapers.
    val BackgroundLight = Color(0xFFF8F9FA)
    val SurfaceLight = Color(0xFFFFFFFF)           // Cards, sheets
    val SurfaceSecondaryLight = Color(0xFFF1F3F5)  // Headers, bottom bar — 5% lift from bg
    val SurfaceTertiaryLight = Color(0xFFE9ECEF)   // Selected pills, inactive chips

    // ─── Backgrounds & Surfaces — Deep Space (Dark) ─────────────────────
    // Deep navy, not pure black — wallpapers pop, not crushed on OLED.
    val BackgroundDark = Color(0xFF0A0E1A)
    val SurfaceDark = Color(0xFF12151F)             // Cards
    val SurfaceSecondaryDark = Color(0xFF1A1F2E)    // Bottom bar, search field — 12% lift
    val SurfaceTertiaryDark = Color(0xFF232838)     // Inactive chips, search field

    // ─── Surface container levels (Material3 slots) ─────────────────────
    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFF8F9FA)
    val SurfaceContainerLight = Color(0xFFF1F3F5)   // Bottom bar, headers
    val SurfaceContainerHighLight = Color(0xFFE9ECEF) // Search field, chip inactive
    val SurfaceDimLight = Color(0xFFE9ECEF)
    val SurfaceBrightLight = Color(0xFFFFFFFF)

    val SurfaceContainerLowestDark = Color(0xFF080C16)
    val SurfaceContainerLowDark = Color(0xFF12151F)
    val SurfaceContainerDark = Color(0xFF1A1F2E)    // Bottom bar
    val SurfaceContainerHighDark = Color(0xFF232838) // Search field, inactive chips
    val SurfaceDimDark = Color(0xFF080C16)
    val SurfaceBrightDark = Color(0xFF1A1F2E)

    // ─── Text — Dawn ────────────────────────────────────────────────────
    val TextPrimaryLight = Color(0xFF0A0E1A)     // 15.8:1 on #F8F9FA
    val TextSecondaryLight = Color(0xFF495057)   // 7:1
    val TextTertiaryLight = Color(0xFF868E96)    // 4.6:1

    // ─── Text — Deep Space ──────────────────────────────────────────────
    val TextPrimaryDark = Color(0xFFF8F9FA)      // 15.6:1 on #0A0E1A
    val TextSecondaryDark = Color(0xFFADB5BD)    // 7.2:1
    val TextTertiaryDark = Color(0xFF6C757D)     // 4.8:1

    // ─── Separators ─────────────────────────────────────────────────────
    val SeparatorLight = Color(0xFFDEE2E6)       // Hairlines, dividers
    val SeparatorDark = Color(0xFF2A3042)        // 0.3 alpha base for dark

    // ─── Glass (frosted overlays on images) ────────────────────────────
    // Used only where UI floats over wallpaper images (detail screen).
    // Dark translucent fill ensures pills are visible on ANY wallpaper —
    // light, dark, or colored. Matches the bottom panel gradient base.
    val GlassDark = Color.Black.copy(alpha = 0.38f)
    val GlassBorderDark = Color.White.copy(alpha = 0.14f)
}

object KraftSpacing {
    /** 8px rhythm — every value is a multiple of 8 or an exception (4, 14). */
    val Spacing2 = 2.dp
    val Spacing4 = 4.dp
    val Spacing8 = 8.dp
    val Spacing12 = 12.dp
    val Spacing14 = 14.dp // Text field horizontal padding
    val Spacing16 = 16.dp
    val Spacing20 = 20.dp
    val Spacing24 = 24.dp
    val Spacing32 = 32.dp
    val Spacing40 = 40.dp
    val Spacing48 = 48.dp
    val Spacing56 = 56.dp
    val Spacing64 = 64.dp

    /** Screen edge padding on mobile. */
    val ScreenEdge = Spacing16

    /** Minimum tile width for adaptive grid columns (landscape + tablet). */
    val GridTileMin = 150.dp
}

object KraftRadius {
    val Small = 8.dp
    val Standard = 12.dp
    val Large = 16.dp
    val Hero = 20.dp
}

object KraftTypeScale {
    val LargeTitle = 34.sp
    val Title1 = 28.sp
    val Title2 = 22.sp
    val Title3 = 20.sp
    val Headline = 17.sp
    val Body = 17.sp
    val Callout = 16.sp
    val Subheadline = 15.sp
    val Footnote = 13.sp
    val Caption1 = 12.sp
    val Caption2 = 11.sp
}

/** Centralized tuning constants — every magic number lives here for easy auditing. */
object KraftConstants {
    // -- Caching --
    const val SearchCacheTtlMs = 30 * 60 * 1000L // 30 minutes
    const val SearchCacheMaxEntries = 100
    const val FavoriteImageMaxBytes = 1L * 1024 * 1024 * 1024 // 1GB
    const val CoilDiskMaxBytes = 512L * 1024 * 1024 // 512 MB
    const val CoilMemoryPercent = 0.25

    // -- Network --
    const val RetryMax = 3
    const val RetryBackoffBaseMs = 1000L // 1s * (1 << attempt) => 1s,2s,4s
    const val CallTimeoutSec = 30L
    const val ConnectTimeoutSec = 15L
    const val ReadTimeoutSec = 15L
    const val RateLimitCooldownMs = 60_000L

    // -- UI / Grid --
    const val GridPrefetchAhead = 4
    const val GridPrefetchThreshold = 20
    const val GridPrefetchDebounceMs = 150L
    const val MinRefreshMs = 500L

    // -- Crop / Decode --
    const val MaxDecodeDim = 4096
    const val MaxCropZoom = 8f
    const val CropAnimDurationMs = 220L

    // -- Glass overlays --
    const val GlassBlurPx = 20f  // Backdrop blur radius for frosted glass
}
