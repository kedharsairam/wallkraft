package com.wallkraft.app.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Color schemes — clean, neutral, wallpaper-first.
 *
 * Light: pure white backgrounds, black labels, system blue accent.
 * Dark: true black backgrounds, white labels, brighter blue accent.
 */
object KraftColorSchemes {
    val Light = lightColorScheme(
        primary = KraftColors.AccentBlue,
        onPrimary = Color.White,
        primaryContainer = KraftColors.AccentBlue.copy(alpha = KraftConstants.ContainerAlphaLight),
        onPrimaryContainer = KraftColors.AccentBlue,
        secondary = KraftColors.AccentTeal,
        onSecondary = Color.White,
        secondaryContainer = KraftColors.AccentTeal.copy(alpha = KraftConstants.ContainerAlphaLight),
        onSecondaryContainer = KraftColors.AccentTeal,
        tertiary = KraftColors.AccentOrange,
        onTertiary = Color.White,
        error = KraftColors.AccentRed,
        onError = Color.White,
        errorContainer = KraftColors.AccentRed.copy(alpha = KraftConstants.ContainerAlphaLight),
        onErrorContainer = KraftColors.AccentRed,
        background = KraftColors.BackgroundLight,
        onBackground = KraftColors.TextPrimaryLight,
        surface = KraftColors.SurfaceLight,
        onSurface = KraftColors.TextPrimaryLight,
        surfaceVariant = KraftColors.SurfaceTertiaryLight,
        onSurfaceVariant = KraftColors.TextSecondaryLight,
        surfaceContainerLowest = KraftColors.SurfaceContainerLowestLight,
        surfaceContainerLow = KraftColors.SurfaceContainerLowLight,
        surfaceContainer = KraftColors.SurfaceContainerLight,
        surfaceContainerHigh = KraftColors.SurfaceContainerHighLight,
        surfaceContainerHighest = KraftColors.SurfaceTertiaryLight,
        surfaceDim = KraftColors.SurfaceDimLight,
        surfaceBright = KraftColors.SurfaceBrightLight,
        inverseSurface = KraftColors.TextPrimaryDark,
        inverseOnSurface = KraftColors.BackgroundDark,
        inversePrimary = KraftColors.AccentBlueDark,
        outline = KraftColors.SeparatorLight,
        outlineVariant = KraftColors.SeparatorLight.copy(alpha = KraftConstants.OutlineVariantAlpha),
        scrim = Color.Black,
    )

    val Dark = darkColorScheme(
        primary = KraftColors.AccentBlueDark,
        onPrimary = Color.Black,
        primaryContainer = KraftColors.AccentBlueDark.copy(alpha = KraftConstants.ContainerAlphaDark),
        onPrimaryContainer = KraftColors.AccentBlueDark,
        secondary = KraftColors.AccentTealDark,
        onSecondary = Color.Black,
        secondaryContainer = KraftColors.AccentTealDark.copy(alpha = KraftConstants.ContainerAlphaDark),
        onSecondaryContainer = KraftColors.AccentTealDark,
        tertiary = KraftColors.AccentOrangeDark,
        onTertiary = Color.Black,
        error = KraftColors.AccentRedDark,
        onError = Color.Black,
        errorContainer = KraftColors.AccentRed.copy(alpha = KraftConstants.ContainerAlphaDark),
        onErrorContainer = KraftColors.AccentRedDark,
        background = KraftColors.BackgroundDark,
        onBackground = KraftColors.TextPrimaryDark,
        surface = KraftColors.SurfaceDark,
        onSurface = KraftColors.TextPrimaryDark,
        surfaceVariant = KraftColors.SurfaceTertiaryDark,
        onSurfaceVariant = KraftColors.TextSecondaryDark,
        surfaceContainerLowest = KraftColors.SurfaceContainerLowestDark,
        surfaceContainerLow = KraftColors.SurfaceContainerLowDark,
        surfaceContainer = KraftColors.SurfaceContainerDark,
        surfaceContainerHigh = KraftColors.SurfaceContainerHighDark,
        surfaceContainerHighest = KraftColors.SurfaceTertiaryDark,
        surfaceDim = KraftColors.SurfaceDimDark,
        surfaceBright = KraftColors.SurfaceBrightDark,
        inverseSurface = KraftColors.TextPrimaryLight,
        inverseOnSurface = KraftColors.BackgroundLight,
        inversePrimary = KraftColors.AccentBlue,
        scrim = Color.Black,
        outline = KraftColors.SeparatorDark,
        outlineVariant = KraftColors.SeparatorDark.copy(alpha = KraftConstants.OutlineVariantAlpha),
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

/** Theme wrapper — clean, neutral, wallpaper-first. */
@Composable
fun KraftTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) KraftColorSchemes.Dark else KraftColorSchemes.Light,
        typography = KraftTypography.Typography,
        content = content,
    )
}
