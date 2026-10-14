package com.tott.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCompact = compositionLocalOf { false }
val LocalMotion = compositionLocalOf { true }

fun Color.mix(other: Color, f: Float): Color {
    val c = f.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * c,
        green = green + (other.green - green) * c,
        blue = blue + (other.blue - blue) * c,
        alpha = alpha + (other.alpha - alpha) * c
    )
}

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

/** Accent presets (argb). Default purple keeps the exact 1:1 baseline tokens. */
val AccentPresets = listOf(
    0xFF6750A4L to "Default",
    0xFF0061A4L to "Ocean",
    0xFF006A60L to "Teal",
    0xFF3F6900L to "Leaf",
    0xFF984061L to "Berry",
    0xFF8C4E00L to "Amber"
)

fun accentLight(seed: Color): ColorScheme = lightColorScheme(
    primary = seed,
    onPrimary = White,
    primaryContainer = seed.mix(White, 0.88f),
    onPrimaryContainer = seed.mix(Black, 0.60f),
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceLowest,
    surfaceContainerLow = LightSurfaceLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceHigh,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = seed.mix(White, 0.45f),
    scrim = LightScrim
)

fun accentDark(seed: Color): ColorScheme = darkColorScheme(
    primary = seed.mix(White, 0.50f),
    onPrimary = seed.mix(Black, 0.65f),
    primaryContainer = seed.mix(Black, 0.45f),
    onPrimaryContainer = seed.mix(White, 0.85f),
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = seed,
    scrim = LightScrim
)

/** AMOLED: crush surfaces to black, keep accents. */
fun ColorScheme.amoled(): ColorScheme = copy(
    surface = Black,
    surfaceDim = Black,
    surfaceBright = Color(0xFF1E1E1E),
    surfaceContainerLowest = Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1E1E1E)
)
