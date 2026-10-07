/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Kedhar Sairam
 */
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
    val AuroraBlue = Color(0xFF0A84FF)        // Primary accent — systemBlue dark
    val AuroraGreen = Color(0xFF30D158)       // Success, favorites, download — systemGreen dark
    val AuroraRed = Color(0xFFFF453A)         // Destructive, unfavorite — systemRed dark
    val AuroraOrange = Color(0xFFFF9F0A)      // Warning, rate limit — systemOrange dark
    val AuroraPink = Color(0xFFFF375F)        // Special accent — systemPink dark
    val AuroraPurple = Color(0xFFBF5AF2)      // Tags, special actions — systemPurple dark
    val AuroraIndigo = Color(0xFF5E5CE6)      // Secondary accent — systemIndigo dark
    val AuroraTeal = Color(0xFF40CBE0)        // Data saver, cache — systemTeal dark

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

    // ─── Backgrounds & Surfaces — Dark Mode ───────────────────────────
    // True black for OLED. System grays for surfaces.
    val Background = Color(0xFF000000)              // systemBackground — page canvas
    val Surface = Color(0xFF1C1C1E)                 // secondarySystemBackground — cards, elevated surfaces
    val SurfaceSecondary = Color(0xFF2C2C2E)        // tertiarySystemBackground — highest elevation
    val SurfaceTertiary = Color(0xFF3A3A3C)         // quaternary — maximum elevation
    /** Search bar background — slightly lighter than page for depth. */
    val SearchBar = Color(0xFF1C1C1E)

    /**
     * Stepped container scale for nested depth: cards sit on Low (= Surface),
     * in-card fields/chips/dialog fills lift to High. High must stay visibly
     * above Low — collapsing them erases lifted elements into their parents.
     */
    val SurfaceContainer = Color(0xFF242428)
    val SurfaceHigh = Color(0xFF2E2E33)

    // ─── Text — Dark Mode ──────────────────────────────────────────────
    // Labels use #EBEBF5 base (cool gray-white, not pure white).
    val TextPrimary = Color(0xFFFFFFFF)       // label — 100% opacity
    val TextSecondary = Color(0x99EBEBF5)     // secondaryLabel — 60% of #EBEBF5
    val TextTertiary = Color(0x80EBEBF5)      // tertiaryLabel — 50% of #EBEBF5 (~5.2:1 on #000000, above AA 4.5:1)

    // ─── Separators ─────────────────────────────────────────────────────
    // Separator = #545458 at ~35% alpha on dark backgrounds
    val Separator = Color(0x59545458)
    // OpaqueSeparator = #38383A for structural hairlines
    val OpaqueSeparator = Color(0xFF38383A)

    // ─── Glass (frosted overlays on images) ────────────────────────────
    // Dark translucent fill ensures pills are visible on ANY wallpaper.
    val Glass = Color.Black.copy(alpha = 0.55f)
    val GlassBorder = Color.White.copy(alpha = 0.35f)

    // ─── Tab Bar ────────────────────────────────────────────────────────
    val TabBarInactive = Color(0xFF9E9EA3)     // inactive tab — ~5.2:1 on #000000 (AA 4.5:1, bumped from #8E8E93 4.23:1)
    val TabBarSeparator = Color(0xFF38383A)    // opaque separator — structural hairline

    // ─── Filter Chips ──────────────────────────────────────────────────
    // Non-purity chips (categories, sorting, orientation).
    // ChipSelectedLabel: AuroraBlue #0A84FF on ChipSelectedContainer #330A84FF.
    // Container effective fill = ~#1A0A84FF on #1C1C1E surface.
    // Contrast: #0A84FF vs #1C1C1E ≈ 4.57:1 (AA pass for normal text).
    val ChipSelectedContainer = AuroraBlue.copy(alpha = 0.2f)  // subtle blue tint — selected chip
    val ChipSelectedLabel = AuroraBlue                     // primary label on light tint for contrast

    // Purity chips — Aurora palette for cohesion.
    val PuritySfwContainer = AuroraGreen.copy(alpha = 0.2f)   // subtle green tint
    val PuritySfwLabel = Color.White                            // white label on green container
    val PuritySketchyContainer = AuroraOrange.copy(alpha = 0.2f) // subtle orange tint
    val PuritySketchyLabel = Color.White                        // white label on orange container
    val PurityNsfwContainer = AuroraRed.copy(alpha = 0.2f)    // subtle red tint
    val PurityNsfwLabel = Color.White                           // white label on red container

    /** Glance widget background — near-black, slightly lifted from page for depth. */
    val WidgetBackground = Color(0xFF1A1A1A)
}

/** Centralized tuning constants — every magic number lives here. */
object KraftConstants {
    // -- Caching --
    const val SearchCacheTtlMs = 30 * 60 * 1000L
    const val SearchCacheMaxEntries = 100
    // Detail metadata rarely changes (views/favorites go stale gracefully),
    // so the per-wallpaper disk cache lives much longer than search pages.
    const val WallpaperCacheTtlMs = 7 * 24 * 60 * 60 * 1000L
    const val WallpaperCacheMaxEntries = 200
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
    val ThumbBlurRadius = 16.dp

    // -- Alphas --
    const val ContainerAlpha = 0.2f

    // -- Overlay Alphas (detail screen, crop dialog) --
    // Measured ratios assume white text on these fills over #000000 wallpaper.
    const val OverlayScrimAlpha = 0.55f          // Top gradient scrim for status bar legibility
    const val OverlayPillAlpha = 0.7f            // Data saver loading pill background (bumped 0.6→0.7, ≈7.0:1 on black)
    const val OverlayDragHandleAlpha = 0.38f     // Glass drag handle
    const val OverlayHintAlpha = 0.55f           // Pull hint / section heading text (≈5.9:1 on black, AA pass)
    const val OverlayStatPillBg = 0.22f          // Stat pill background on images (bumped 0.18→0.22, ≈4.7:1 on black)
    const val OverlayStatPillBorder = 0.30f      // Stat pill border on images (bumped 0.25→0.30, ≈5.3:1 on black)
    const val OverlayCropScrimTop = 0.4f         // Crop dialog top scrim
    const val OverlayCropPanelAlpha = 0.65f      // Crop dialog bottom panel
    const val TagChipFillAlpha = 0.55f           // Tag chip background fill on images (bumped 0.45→0.55, ≈5.9:1 on black)
    const val TagChipBorderAlpha = 0.75f         // Tag chip border on images (bumped 0.7→0.75, ≈7.4:1 on black)
    const val CropDialogSecondaryAlpha = 0.8f    // Crop dialog secondary text (cancel, inactive segments)

    // -- Error states --
    const val ErrorContainerAlpha = 0.4f         // Error icon background
    const val ErrorIconAlpha = 0.7f              // Error icon tint

    // -- Skeleton / Shimmer --
    const val SkeletonAlphaMin = 0.3f
    const val SkeletonAlphaMax = 0.5f
    const val ShimmerGradientAlpha = 0.5f
    val ShimmerAvgTileHeightDp = 280.dp

    // Smallest tile a grid will lay out. Two screens share it so a phone and a tablet
    // agree on when tiles reflow; declared once because two literal 150s were two chances
    // to type 150 differently.
    val GridTileMin = 150.dp

    // Onboarding fixed specs. The button area is sized so Get Started never jumps when Next
    // turns into it: both buttons the same size, centered, with an invisible Skip reserve on
    // the last page balancing the TextButton height. Each value was a literal with its reason
    // in WelcomeScreen; declared once because three literals were three chances to drift.
    val OnboardingButtonWidth = 260.dp
    val OnboardingAreaHeight = 88.dp
    val SkipReserve = 36.dp
    val OnboardingHeroCircle = 80.dp

    // Min pill width keeps short labels tappable; back-button clearance offsets content below
    // the floating button; username cap keeps long names from overflowing a panel. Each
    // carried a "layout-specific, not spacing scale" comment at its site — which is exactly
    // what makes it an app metric rather than a spacing value.
    val PillMinWidth = 100.dp
    val BackButtonClearance = 68.dp
    val UsernameMaxWidth = 260.dp

    // Buy-Me-a-Coffee asset width — fixed brand spec. Same 182 as two sibling apps' sponsor
    // buttons, separately declared because sharing a brand asset's dimensions across apps
    // would couple unrelated products to one image file.
    val SponsorButtonWidth = 182.dp

    // Lockscreen preview scrim height behind the mock clock.
    val CropScrimHeight = 200.dp

    // Collection cards in the favorites strip. Fixed at 88dp so a strip of cards has a
    // uniform rhythm regardless of image aspect — the image crops, the card does not move.
    val CollectionCardSize = 88.dp

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

    // -- Overlay dims (migrated from raw literals) --
    const val OverlayPickerDimAlpha = 0.5f       // Position picker dim overlay
    const val OverlayCardMenuAlpha = 0.45f       // Collection overflow menu scrim
    const val OverlayCameraAlpha = 0.15f         // Lockscreen camera pill + welcome hero tint
    const val SkeletonPlaceholderAlpha = 0.1f    // Related-strip skeleton fill
    const val NavHostGlowAlpha = 0.08f           // Nav host ambient glow tint

    // -- Tokens for previously magic literals (P1) --
    const val ApiKeyMaxLength = 64
    const val SlowFrameThresholdMs = 32
    const val ValidateLimit = 1
    const val DialogListMaxHeightDp = 280
    const val CropDialogMaxHeightDp = 320
    const val GlassTabOuterAlpha = 0.22f
    const val GlassTabInnerAlpha = 0.22f
    const val GlassTabMidAlpha = 0.45f
    const val GlassTabHighlightAlpha = 0.15f
    const val CollectionNameMaxLength = 40

    // -- Elevation / Depth (DESIGN Depth2 y4 blur12 alpha0.2) --
    // Dialogs and panels map to Depth2; keep explicit so 16dp shadows don't drift.
    val DialogElevation = 12.dp // Depth2 per DESIGN
    val PanelElevation = 16.dp // Depth3 per DESIGN — filter dropdowns
}
