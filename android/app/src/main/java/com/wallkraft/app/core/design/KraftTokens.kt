package com.wallkraft.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * WallKraft — Design Tokens (dark only).
 *
 * True black (#000000) OLED backgrounds. System grays for surfaces.
 * White labels, brighter accent colors. Wallpaper-first.
 */
object KraftColors {
    // ─── Brand Colors ──────────────────────────────────────────────────
    // WallKraft's identity: Aurora palette inspired by the Northern Lights.
    // Dark-only: use the Dark variants directly.
    val AuroraBlue = Color(0xFF0A84FF)        // Primary accent — Apple HIG systemBlue dark
    val AuroraGreen = Color(0xFF30D158)       // Success, favorites, download — Apple HIG systemGreen dark
    val AuroraRed = Color(0xFFFF453A)         // Destructive, unfavorite — Apple HIG systemRed dark
    val AuroraOrange = Color(0xFFFF9F0A)      // Warning, rate limit — Apple HIG systemOrange dark
    val AuroraPink = Color(0xFFFF375F)        // Special accent — Apple HIG systemPink dark
    val AuroraPurple = Color(0xFFBF5AF2)      // Tags, special actions — Apple HIG systemPurple dark
    val AuroraIndigo = Color(0xFF5E5CE6)      // Secondary accent — Apple HIG systemIndigo dark
    val AuroraTeal = Color(0xFF40CBE0)        // Data saver, cache — Apple HIG systemTeal dark

    // ─── Legacy aliases ────────────────────────────────────────────────
    // Kept for backwards compatibility. New code should use Aurora* names.
    val AccentBlue = AuroraBlue
    val AccentGreen = AuroraGreen
    val AccentRed = AuroraRed
    val AccentOrange = AuroraOrange
    val AccentPink = AuroraPink
    val AccentPurple = AuroraPurple
    val AccentIndigo = AuroraIndigo
    val AccentTeal = AuroraTeal

    // ─── Backgrounds & Surfaces — Apple HIG Dark Mode ──────────────────
    // True black for OLED. System grays for surfaces.
    val Background = Color(0xFF000000)              // systemBackground — page canvas
    val Surface = Color(0xFF1C1C1E)                 // secondarySystemBackground — cards, elevated surfaces
    val SurfaceSecondary = Color(0xFF2C2C2E)        // tertiarySystemBackground — highest elevation
    val SurfaceTertiary = Color(0xFF3A3A3C)         // quaternary — maximum elevation
    /** Search bar background — slightly lighter than page for depth. */
    val SearchBar = Color(0xFF1C1C1E)

    // ─── Text — Apple HIG Dark Mode ────────────────────────────────────
    // Apple HIG: labels use #EBEBF5 base (cool gray-white, not pure white).
    val TextPrimary = Color(0xFFFFFFFF)       // label — 100% opacity
    val TextSecondary = Color(0x99EBEBF5)     // secondaryLabel — 60% of #EBEBF5
    val TextTertiary = Color(0x4CEBEBF5)      // tertiaryLabel — 30% of #EBEBF5

    // ─── Separators ─────────────────────────────────────────────────────
    // Apple HIG: separator = #545458 at ~35% alpha on dark backgrounds
    val Separator = Color(0x59545458)
    // Apple HIG: opaqueSeparator = #38383A for structural hairlines
    val OpaqueSeparator = Color(0xFF38383A)

    // ─── Glass (frosted overlays on images) ────────────────────────────
    // Dark translucent fill ensures pills are visible on ANY wallpaper.
    val Glass = Color.Black.copy(alpha = 0.55f)
    val GlassBorder = Color.White.copy(alpha = 0.25f)

    // ─── Tab Bar ────────────────────────────────────────────────────────
    val TabBarInactive = Color(0xFF8E8E93)     // standard inactive tab — ~4.2:1 on #000000
    val TabBarSeparator = Color(0xFF38383A)    // opaque separator — structural hairline

    // ─── Filter Chips ──────────────────────────────────────────────────
    // Non-purity chips (categories, sorting, orientation).
    val ChipSelectedContainer = AuroraBlue.copy(alpha = 0.2f)  // subtle blue tint — Apple HIG selected chip
    val ChipSelectedLabel = AuroraBlue                         // full system blue label

    // Purity chips — Aurora palette for cohesion.
    val PuritySfwContainer = AuroraGreen.copy(alpha = 0.2f)   // subtle green tint
    val PuritySfwLabel = AuroraGreen                           // full green label
    val PuritySketchyContainer = AuroraOrange.copy(alpha = 0.2f) // subtle orange tint
    val PuritySketchyLabel = AuroraOrange                      // full orange label
    val PurityNsfwContainer = AuroraRed.copy(alpha = 0.2f)    // subtle red tint
    val PurityNsfwLabel = AuroraRed                            // full red label
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

    /** Standard touch target — 44dp (accessibility minimum). */
    val TouchTarget = 44.dp

    /** Search bar height — matches TouchTarget for visual consistency. */
    val SearchBarHeight = TouchTarget

    /** Top bar height. */
    val TopBarHeight = TouchTarget

    /** Standard border width — 1dp hairline. */
    val BorderWidth = 1.dp

    /** Progress bar / loading indicator height. */
    val ProgressBarHeight = 2.dp

    /** Spinner stroke width. */
    val SpinnerStroke = 2.dp

    /** Avatar size — 32dp circular. */
    val AvatarSize = 32.dp
}

object KraftRadius {
    /** Standard corner radii — system-wide consistency. */
    val Small = 8.dp       // Small elements: chips, badges
    val Standard = 10.dp   // Cards, list items — standard card radius
    val Large = 14.dp      // Large cards, search bars
    val Hero = 22.dp       // Sheets, modals — sheet radius
    val Pill = 50.dp       // Fully rounded — buttons, tags, filters
    val DragHandle = 2.5.dp // Drag handle radius
}

object KraftIconSize {
    /** Standard icon sizes — consistent across all screens. */
    val Tiny = 12.dp       // Badges, indicators
    val Small = 16.dp      // Inline icons, arrows
    val Medium = 20.dp     // Button icons, toolbar icons
    val Large = 24.dp      // Selection badges, prominent icons
    val XLarge = 40.dp     // Empty state, error icons
    val TabBar = 25.dp     // Tab bar icons — standard platform size
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

    /** Letter spacing for section headings and labels. */
    val LabelSpacing = 0.4.sp
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
    const val ContainerAlpha = 0.2f

    // -- Overlay Alphas (detail screen, crop dialog) --
    const val OverlayScrimAlpha = 0.55f          // Top gradient scrim for status bar legibility
    const val OverlayPillAlpha = 0.6f            // Data saver loading pill background
    const val OverlayDragHandleAlpha = 0.38f     // Glass drag handle
    const val OverlayHintAlpha = 0.55f           // Pull hint / section heading text
    const val OverlayStatPillBg = 0.18f          // Stat pill background on images (increased for contrast)
    const val OverlayStatPillBorder = 0.25f      // Stat pill border on images (increased for contrast)
    const val OverlayCropScrimTop = 0.4f         // Crop dialog top scrim
    const val OverlayCropPanelAlpha = 0.65f      // Crop dialog bottom panel
    const val TagChipFillAlpha = 0.45f           // Tag chip background fill on images
    const val TagChipBorderAlpha = 0.7f          // Tag chip border on images
    const val CropDialogSecondaryAlpha = 0.8f    // Crop dialog secondary text (cancel, inactive segments)

    // -- Error states --
    const val ErrorContainerAlpha = 0.4f         // Error icon background
    const val ErrorIconAlpha = 0.7f              // Error icon tint

    // -- Skeleton / Shimmer --
    const val SkeletonAlphaMin = 0.3f
    const val SkeletonAlphaMax = 0.5f
    const val ShimmerGradientAlpha = 0.5f

    // -- Badges --
    const val BadgeAlpha = 0.85f                 // Downloaded badge
    const val SelectionOverlayAlpha = 0.4f       // Selection check overlay

    // -- Card / Surface --
    const val SurfaceVariantAlpha = 0.4f         // Empty state icon background
    const val IconTintAlpha = 0.85f              // Empty state icon tint — clearly visible
    const val EmptyStateIconBgAlpha = 0.40f      // Empty state circle — clearly visible on black

    // -- Deleted uploader --
    const val DeletedUploaderBgAlpha = 0.12f     // Deleted account avatar background
    const val DeletedUploaderTextAlpha = 0.5f    // Deleted account name + icon tint
}
