package com.wallkraft.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WallKraft — Design Tokens.
 *
 * Every color, spacing, radius, and type scale value matches the standard human
 * Interface Guidelines. Wallpapers are the hero; the UI is a clean, neutral
 * canvas that gets out of the way.
 *
 * Dark mode uses true black (#000000) for OLED — wallpapers pop, no gray haze.
 * Light mode uses pure white (#FFFFFF) — crisp, clean, iOS-native feel.
 */
object KraftColors {
    // ─── System Colors — Standard Palette ──────────────────────────────
    val AccentBlue = Color(0xFF007AFF)        // iOS system blue
    val AccentBlueDark = Color(0xFF0A84FF)    // Dark mode blue (slightly lighter)
    val AccentGreen = Color(0xFF34C759)       // iOS system green
    val AccentGreenDark = Color(0xFF32D74B)   // Dark mode green
    val AccentRed = Color(0xFFFF3B30)         // iOS system red
    val AccentRedDark = Color(0xFFFF453A)     // Dark mode red
    val AccentOrange = Color(0xFFFF9500)      // iOS system orange
    val AccentOrangeDark = Color(0xFFFF9F0A)  // Dark mode orange
    val AccentPink = Color(0xFFFF2D55)        // iOS system pink
    val AccentPinkDark = Color(0xFFFF375D)    // Dark mode pink
    val AccentPurple = Color(0xFFAF52DE)      // iOS system purple
    val AccentPurpleDark = Color(0xFFBF5AF2)  // Dark mode purple
    val AccentIndigo = Color(0xFF5856D6)      // iOS system indigo
    val AccentIndigoDark = Color(0xFF5E5CE6)  // Dark mode indigo
    val AccentTeal = Color(0xFF00C7BE)        // iOS system teal
    val AccentTealDark = Color(0xFF64D2FF)    // Dark mode teal

    // ─── Backgrounds & Surfaces — Light (iOS Light Mode) ────────────────
    // Pure white background, system gray groupings.
    val BackgroundLight = Color(0xFFFFFFFF)           // systemBackground
    val SurfaceLight = Color(0xFFFFFFFF)              // secondarySystemBackground
    val SurfaceSecondaryLight = Color(0xFFF2F2F7)     // tertiarySystemBackground
    val SurfaceTertiaryLight = Color(0xFFE5E5EA)      // quaternarySystemBackground

    // ─── Backgrounds & Surfaces — Dark (iOS Dark Mode) ──────────────────
    // True black for OLED. System grays for surfaces.
    val BackgroundDark = Color(0xFF000000)             // systemBackground
    val SurfaceDark = Color(0xFF1C1C1E)                // secondarySystemBackground
    val SurfaceSecondaryDark = Color(0xFF2C2C2E)       // tertiarySystemBackground
    val SurfaceTertiaryDark = Color(0xFF3A3A3C)        // quaternarySystemBackground

    // ─── Surface container levels (Material3 slots) ─────────────────────
    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFF2F2F7)
    val SurfaceContainerLight = Color(0xFFE5E5EA)
    val SurfaceContainerHighLight = Color(0xFFE5E5EA)
    val SurfaceDimLight = Color(0xFFE5E5EA)
    val SurfaceBrightLight = Color(0xFFFFFFFF)

    val SurfaceContainerLowestDark = Color(0xFF000000)
    val SurfaceContainerLowDark = Color(0xFF1C1C1E)
    val SurfaceContainerDark = Color(0xFF2C2C2E)
    val SurfaceContainerHighDark = Color(0xFF3A3A3C)
    val SurfaceDimDark = Color(0xFF000000)
    val SurfaceBrightDark = Color(0xFF2C2C2E)

    // ─── Text — Light Mode ──────────────────────────────────────────────
    val TextPrimaryLight = Color(0xFF000000)     // label — 100% opacity
    val TextSecondaryLight = Color(0x99000000)   // secondaryLabel — 60% opacity
    val TextTertiaryLight = Color(0x4C000000)    // tertiaryLabel — 30% opacity

    // ─── Text — Dark Mode ───────────────────────────────────────────────
    val TextPrimaryDark = Color(0xFFFFFFFF)       // label — 100% opacity
    val TextSecondaryDark = Color(0x99FFFFFF)     // secondaryLabel — 60% opacity
    val TextTertiaryDark = Color(0x4CFFFFFF)      // tertiaryLabel — 30% opacity

    // ─── Separators ─────────────────────────────────────────────────────
    val SeparatorLight = Color(0x4D3C3C43)       // separator — 30% of #3C3C43
    val SeparatorDark = Color(0x4D3C3C43)         // separator — 30% of #3C3C43

    // ─── Glass (frosted overlays on images) ────────────────────────────
    // Dark translucent fill ensures pills are visible on ANY wallpaper.
    val GlassDark = Color.Black.copy(alpha = 0.38f)
    val GlassBorderDark = Color.White.copy(alpha = 0.14f)

    // ─── Tab Bar ────────────────────────────────────────────────────────
    val TabBarInactive = Color(0xFF8E8E93)     // standard inactive tab
    val TabBarSeparator = Color(0x3C000000)    // 23% black — standard hairline
}

object KraftSpacing {
    /** 8px rhythm — standard spacing scale. */
    val Spacing2 = 2.dp
    val Spacing4 = 4.dp
    val Spacing8 = 8.dp
    val Spacing12 = 12.dp
    val Spacing16 = 16.dp
    val Spacing20 = 20.dp
    val Spacing24 = 24.dp
    val Spacing32 = 32.dp
    val Spacing40 = 40.dp
    val Spacing48 = 48.dp
    val Spacing56 = 56.dp
    val Spacing64 = 64.dp

    /** Screen edge padding — 16dp on standard phones. */
    val ScreenEdge = Spacing16

    /** Minimum tile width for adaptive grid columns. */
    val GridTileMin = 150.dp
    /** Top bar height. */
    val TopBarHeight = 44.dp
}

object KraftRadius {
    /** Standard corner radii — system-wide consistency. */
    val Small = 8.dp       // Small elements: chips, badges
    val Standard = 10.dp   // Cards, list items — standard card radius
    val Large = 14.dp      // Large cards, search bars
    val Hero = 22.dp       // Sheets, modals — sheet radius
}

object KraftTypeScale {
    /**
     * Type scale — maps to SF Pro sizes.
     * Android system font (Roboto) is close enough; the scale and weights
     * matter more than the exact typeface.
     */
    val LargeTitle = 34.sp   // .largeTitle
    val Title1 = 28.sp       // .title1
    val Title2 = 22.sp       // .title2
    val Title3 = 20.sp       // .title3
    val Headline = 17.sp     // .headline
    val Body = 17.sp         // .body
    val Callout = 16.sp      // .callout
    val Subheadline = 15.sp  // .subheadline
    val Footnote = 13.sp     // .footnote
    val Caption1 = 12.sp     // .caption1
    val Caption2 = 11.sp     // .caption2
}

/** Centralized tuning constants — every magic number lives here. */
object KraftConstants {
    // -- Caching --
    const val SearchCacheTtlMs = 30 * 60 * 1000L
    const val SearchCacheMaxEntries = 100
    const val FavoriteImageMaxBytes = 100L * 1024 * 1024
    const val CoilDiskMaxBytes = 512L * 1024 * 1024
    const val CoilMemoryPercent = 0.25

    // -- Network --
    const val RetryMax = 3
    const val RetryBackoffBaseMs = 1000L
    const val CallTimeoutSec = 30L
    const val ConnectTimeoutSec = 15L
    const val ReadTimeoutSec = 15L
    const val RateLimitCooldownMs = 60_000L
    const val RateLimitDefaultRemaining = 45

    // -- UI / Grid --
    const val GridPrefetchAhead = 4
    const val GridPrefetchThreshold = 20
    const val GridPrefetchDebounceMs = 150L
    const val MinRefreshMs = 500L

    // -- Crop / Decode --
    const val MaxDecodeDim = 4096
    const val MaxCropZoom = 8f
    const val CropAnimDurationMs = 220L

    // -- Alphas --
    const val ContainerAlphaLight = 0.12f
    const val ContainerAlphaDark = 0.2f
    const val OutlineVariantAlpha = 0.3f
    const val DividerAlpha = 0.4f

}
