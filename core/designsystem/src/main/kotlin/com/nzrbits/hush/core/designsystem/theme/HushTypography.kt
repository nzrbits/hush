package com.nzrbits.hush.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nzrbits.hush.core.common.model.FontFamilyChoice
import com.nzrbits.hush.core.designsystem.R

/**
 * Hush text roles. Sizes are multiplied by the user's font scale. A screen should use at most
 * three of them at once: home = clock / listItem / body, secondary screens = title / body / caption.
 */
@Immutable
data class HushTypography(
    val clock: TextStyle,
    val hero: TextStyle,
    val date: TextStyle,
    val title: TextStyle,
    val section: TextStyle,
    val listItem: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val bubble: TextStyle,
)

/** Nunito is a variable font; each weight is requested through a variation setting. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val nunito: FontFamily = FontFamily(
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Silkscreen (OFL) for the clock in every mode: hard pixel grid, digits stay distinct at any size. */
private val pixel: FontFamily = FontFamily(
    Font(R.font.silkscreen_regular, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold),
)

/** DEFAULT follows the mode: rounded Nunito for Cozy, the system sans for Minimal. */
fun fontFamilyFor(choice: FontFamilyChoice, minimal: Boolean): FontFamily = when (choice) {
    FontFamilyChoice.DEFAULT -> if (minimal) FontFamily.SansSerif else nunito
    FontFamilyChoice.SYSTEM -> FontFamily.SansSerif
    FontFamilyChoice.NUNITO -> nunito
    FontFamilyChoice.MONO -> FontFamily.Monospace
}

fun hushTypography(family: FontFamily, scale: Float, minimal: Boolean): HushTypography {
    // Line height 1.45 keeps multi-line explanations readable at 13 to 16 sp.
    fun style(size: Int, weight: FontWeight, lineHeight: Float = size * 1.45f) = TextStyle(
        fontFamily = family,
        fontSize = (size * scale).sp,
        fontWeight = weight,
        lineHeight = (lineHeight * scale).sp,
    )
    val clockWeight = if (minimal) FontWeight.Normal else FontWeight.Bold
    val heroWeight = if (minimal) FontWeight.Light else FontWeight.ExtraBold
    return HushTypography(
        // One digit per flip tile, so the size is per tile, not per line.
        clock = TextStyle(fontFamily = pixel, fontSize = (44 * scale).sp, fontWeight = clockWeight, lineHeight = (48 * scale).sp),
        hero = style(40, heroWeight, 44f),
        date = style(16, FontWeight.Normal),
        title = style(24, FontWeight.Bold, 30f),
        section = style(13, FontWeight.SemiBold),
        listItem = style(22, if (minimal) FontWeight.Normal else FontWeight.SemiBold, 30f),
        body = style(16, FontWeight.Normal),
        caption = style(13, FontWeight.Normal),
        bubble = style(16, FontWeight.SemiBold),
    )
}

fun HushTypography.toMaterial(): Typography = Typography(
    displayLarge = clock,
    headlineMedium = title,
    titleLarge = title,
    titleMedium = listItem,
    bodyLarge = body,
    bodyMedium = body,
    labelLarge = section,
    labelMedium = caption,
    bodySmall = caption,
)

val LocalHushTypography = staticCompositionLocalOf {
    hushTypography(FontFamily.SansSerif, 1f, minimal = true)
}
