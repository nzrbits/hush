package com.nzrbits.hush.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.common.model.AppearanceSettings
import com.nzrbits.hush.core.common.model.CozyPalette
import com.nzrbits.hush.core.common.model.ThemeMode

/** Corner radii. Cozy uses the 22 / 14 px radii from the web apps, Minimal stays square. */
@Immutable
data class HushShapes(val card: Dp, val small: Dp, val pill: Dp)

val LocalHushShapes = staticCompositionLocalOf { HushShapes(0.dp, 0.dp, 0.dp) }
val LocalReduceMotion = staticCompositionLocalOf { false }

object HushTheme {
    val colors: HushColors
        @Composable get() = LocalHushColors.current
    val typography: HushTypography
        @Composable get() = LocalHushTypography.current
    val shapes: HushShapes
        @Composable get() = LocalHushShapes.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

fun resolveColors(appearance: AppearanceSettings, systemDark: Boolean): HushColors = when (appearance.themeMode) {
    ThemeMode.MINIMAL -> HushPalettes.Minimal
    ThemeMode.COZY -> when (appearance.cozyPalette) {
        CozyPalette.LIGHT -> HushPalettes.CozyLight
        CozyPalette.DARK -> HushPalettes.CozyDark
        CozyPalette.AUTO -> if (systemDark) HushPalettes.CozyDark else HushPalettes.CozyLight
    }
}

@Composable
fun HushTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit,
) {
    val colors = resolveColors(appearance, isSystemInDarkTheme())
    val minimal = colors.isMinimal
    val family = fontFamilyFor(appearance.fontFamily, minimal)
    val typography = hushTypography(family, appearance.fontScale.factor, minimal)
    val shapes = if (minimal) HushShapes(0.dp, 0.dp, 0.dp) else HushShapes(22.dp, 14.dp, 999.dp)

    CompositionLocalProvider(
        LocalHushColors provides colors,
        LocalHushTypography provides typography,
        LocalHushShapes provides shapes,
        LocalReduceMotion provides appearance.reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = typography.toMaterial(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(shapes.small),
                small = RoundedCornerShape(shapes.small),
                medium = RoundedCornerShape(shapes.card),
                large = RoundedCornerShape(shapes.card),
                extraLarge = RoundedCornerShape(shapes.card),
            ),
            content = content,
        )
    }
}

private fun HushColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = accentText,
        primaryContainer = accentSoft,
        onPrimaryContainer = text,
        secondary = muted,
        onSecondary = background,
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = text,
        tertiaryContainer = accentSoft,
        onTertiaryContainer = text,
        surfaceTint = accent,
        background = background,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = muted,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        outline = lineStrong,
        outlineVariant = line,
        error = danger,
        onError = background,
        tertiary = accent,
        onTertiary = accentText,
        inverseSurface = text,
        inverseOnSurface = background,
    )
}
