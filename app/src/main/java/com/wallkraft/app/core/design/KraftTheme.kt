package com.wallkraft.app.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Light and dark color schemes for WallKraft, built from [KraftColors]. */
object KraftColorSchemes {
    val Light = lightColorScheme(
        primary = KraftColors.AccentBlue,
        onPrimary = Color.White,
        primaryContainer = KraftColors.AccentBlue.copy(alpha = 0.15f),
        onPrimaryContainer = KraftColors.AccentBlueDark,
        secondary = KraftColors.AccentPurple,
        onSecondary = Color.White,
        secondaryContainer = KraftColors.AccentPurple.copy(alpha = 0.15f),
        onSecondaryContainer = KraftColors.AccentPurple,
        tertiary = KraftColors.AccentOrange,
        onTertiary = Color.White,
        error = KraftColors.AccentRed,
        onError = Color.White,
        errorContainer = KraftColors.AccentRed.copy(alpha = 0.15f),
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
        outline = KraftColors.SeparatorLight,
        outlineVariant = KraftColors.SeparatorLight.copy(alpha = 0.3f),
    )

    val Dark = darkColorScheme(
        primary = KraftColors.AccentBlueDark,
        onPrimary = Color.White,
        primaryContainer = KraftColors.AccentBlue.copy(alpha = 0.25f),
        onPrimaryContainer = KraftColors.AccentBlueDark,
        secondary = KraftColors.AccentPurpleDark,
        onSecondary = Color.White,
        secondaryContainer = KraftColors.AccentPurple.copy(alpha = 0.25f),
        onSecondaryContainer = KraftColors.AccentPurpleDark,
        tertiary = KraftColors.AccentOrangeDark,
        onTertiary = Color.White,
        error = KraftColors.AccentRedDark,
        onError = Color.White,
        errorContainer = KraftColors.AccentRed.copy(alpha = 0.25f),
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
        outline = KraftColors.SeparatorDark,
        outlineVariant = KraftColors.SeparatorDark.copy(alpha = 0.3f),
    )
}

/** Typography following the Kraft design scale. */
object KraftTypography {
    val Typography = Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
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
            lineHeight = 24.sp,
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = KraftTypeScale.Callout,
            lineHeight = 22.sp,
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
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = KraftTypeScale.Caption2,
            letterSpacing = 0.2.sp,
        ),
    )
}

/** Wraps [MaterialTheme] with the Kraft design system for the given [darkTheme]. */
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
