package com.wallkraft.app.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Color scheme — dark only, OLED-optimized.
 *
 * True black (#000000) backgrounds. System grays for surfaces.
 * White labels, brighter accent colors. Wallpaper-first.
 */
object KraftColorSchemes {
    val Dark = darkColorScheme(
        primary = KraftColors.AccentBlue,
        onPrimary = Color.Black,
        primaryContainer = KraftColors.AccentBlue.copy(alpha = KraftConstants.ContainerAlpha),
        onPrimaryContainer = KraftColors.AccentBlue,
        secondary = KraftColors.AccentTeal,
        onSecondary = Color.Black,
        secondaryContainer = KraftColors.AccentTeal.copy(alpha = KraftConstants.ContainerAlpha),
        onSecondaryContainer = KraftColors.AccentTeal,
        tertiary = KraftColors.AccentOrange,
        onTertiary = Color.Black,
        error = KraftColors.AccentRed,
        onError = Color.Black,
        errorContainer = KraftColors.AccentRed.copy(alpha = KraftConstants.ContainerAlpha),
        onErrorContainer = KraftColors.AccentRed,
        // Page background = #000000 (OLED true black)
        background = Color.Black,
        onBackground = KraftColors.TextPrimary,
        // Surface = #1C1C1E (card surfaces — elevated above page)
        surface = KraftColors.Surface,
        onSurface = KraftColors.TextPrimary,
        // surfaceVariant = #2C2C2E (tertiarySystemGroupedBackground)
        surfaceVariant = KraftColors.SurfaceSecondary,
        onSurfaceVariant = KraftColors.TextSecondary,
        // Grouped layout: page is #000000, cards are #1C1C1E
        surfaceContainerLowest = Color.Black,                         // #000000 — page background
        surfaceContainerLow = KraftColors.Surface,                    // #1C1C1E — card surface
        surfaceContainer = Color.Black,                               // #000000 — page background
        surfaceContainerHigh = KraftColors.Surface,                   // #1C1C1E — elevated (search bar, chips)
        surfaceContainerHighest = KraftColors.SurfaceSecondary,       // #2C2C2E — highest elevation
        surfaceDim = Color.Black,                                     // #000000 — dimmed = page
        surfaceBright = KraftColors.Surface,                          // #1C1C1E — brightest = cards
        inverseSurface = KraftColors.TextPrimary,
        inverseOnSurface = Color.Black,
        inversePrimary = KraftColors.AccentBlue,
        scrim = Color.Black,
        // Separator = outline color (~35% alpha of #545458)
        outline = KraftColors.Separator,
        // outlineVariant = opaque separator for less prominent borders
        outlineVariant = KraftColors.OpaqueSeparator,
    )
}

/**
 * Typography — maps to standard type scale.
 *
 * We use the system default font (Roboto on Android) with weights and sizes
 * that match the standard SF Pro scale. The visual difference is minimal; the hierarchy
 * and spacing are what matter.
 */
object KraftTypography {
    val Typography = Typography(
        displayLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = KraftTypeScale.LargeTitle,
            lineHeight = 41.sp,
        ),
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = KraftTypeScale.Title1,
            lineHeight = 34.sp,
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = KraftTypeScale.Title2,
            lineHeight = 28.sp,
        ),
        headlineSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = KraftTypeScale.Title3,
            lineHeight = 25.sp,
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = KraftTypeScale.Headline,
            lineHeight = 22.sp,
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = KraftTypeScale.Callout,
            lineHeight = 21.sp,
        ),
        titleSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = KraftTypeScale.Subheadline,
            lineHeight = 20.sp,
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = KraftTypeScale.Body,
            lineHeight = 22.sp,
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = KraftTypeScale.Callout,
            lineHeight = 21.sp,
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = KraftTypeScale.Subheadline,
            lineHeight = 20.sp,
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = KraftTypeScale.Subheadline,
            letterSpacing = 0.2.sp,
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = KraftTypeScale.Footnote,
            letterSpacing = 0.2.sp,
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = KraftTypeScale.Caption2,
            letterSpacing = 0.2.sp,
        ),
    )
}

/** Theme wrapper — dark only, OLED-optimized. */
@Composable
fun KraftTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KraftColorSchemes.Dark,
        typography = KraftTypography.Typography,
        content = content,
    )
}
