package com.wallkraft.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for WallKraft.
 *
 * Follows the Apple-inspired Kraft Design Language from kraft-standards.
 * Every color, spacing, and size constant lives here — never hardcoded in composables.
 */
object KraftColors {
    // -- Accents --
    val AccentBlue = Color(0xFF007AFF)
    val AccentBlueDark = Color(0xFF0A84FF)
    val AccentGreen = Color(0xFF34C759)
    val AccentGreenDark = Color(0xFF30D158)
    val AccentRed = Color(0xFFFF3B30)
    val AccentRedDark = Color(0xFFFF453A)
    val AccentOrange = Color(0xFFFF9500)
    val AccentOrangeDark = Color(0xFFFF9F0A)
    val AccentYellow = Color(0xFFFFCC00)
    val AccentYellowDark = Color(0xFFFFD60A)
    val AccentPurple = Color(0xFFAF52DE)
    val AccentPurpleDark = Color(0xFFBF5AF2)

    // -- Backgrounds & Surfaces --
    val BackgroundLight = Color(0xFFF2F2F7)
    val BackgroundDark = Color(0xFF000000)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF1C1C1E)
    val SurfaceSecondaryLight = Color(0xFFF2F2F7)
    val SurfaceSecondaryDark = Color(0xFF2C2C2E)
    val SurfaceTertiaryLight = Color(0xFFE5E5EA)
    val SurfaceTertiaryDark = Color(0xFF3A3A3C)

    // -- Surface container levels (iOS grouped-table neutrals). Material3
    // leaves these slots to a baseline *purple* palette unless we define them;
    // the bottom bar, sheets, and chips must stay neutral gray. --
    val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
    val SurfaceContainerLowLight = Color(0xFFF2F2F7)
    val SurfaceContainerLight = Color(0xFFEFEFF4)
    val SurfaceContainerHighLight = Color(0xFFE5E5EA)
    val SurfaceDimLight = Color(0xFFE5E5EA)
    val SurfaceBrightLight = Color(0xFFFFFFFF)

    val SurfaceContainerLowestDark = Color(0xFF0A0A0C)
    val SurfaceContainerLowDark = Color(0xFF1C1C1E)
    val SurfaceContainerDark = Color(0xFF2C2C2E)
    val SurfaceContainerHighDark = Color(0xFF38383A)
    val SurfaceDimDark = Color(0xFF141416)
    val SurfaceBrightDark = Color(0xFF2C2C2E)

    // -- Text --
    val TextPrimaryLight = Color(0xFF000000)
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryLight = Color(0xFF3A3A3C)
    val TextSecondaryDark = Color(0xFF8E8E93)
    val TextTertiaryLight = Color(0xFF8E8E93)
    val TextTertiaryDark = Color(0xFF5E5E63)

    // -- Separators & Fills --
    val SeparatorLight = Color(0xFFC6C6C8)
    val SeparatorDark = Color(0xFF38383A)
}

object KraftSpacing {
    /** 8px rhythm — every value is a multiple of 8 or an exception (4, 14). */
    val Spacing2 = 2.dp
    val Spacing4 = 4.dp
    val Spacing8 = 8.dp
    val Spacing12 = 12.dp
    val Spacing14 = 14.dp // Text field horizontal padding (Apple HIG)
    val Spacing16 = 16.dp
    val Spacing20 = 20.dp
    val Spacing24 = 24.dp
    val Spacing32 = 32.dp
    val Spacing40 = 40.dp
    val Spacing48 = 48.dp
    val Spacing64 = 64.dp

    /** Screen edge padding on mobile. */
    val ScreenEdge = Spacing16
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
