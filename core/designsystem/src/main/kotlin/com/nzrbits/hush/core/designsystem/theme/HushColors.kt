package com.nzrbits.hush.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Hush colour roles. Both modes fill the same roles so screens never branch on the mode.
 * Cozy values are taken from the Mr. Nook / Melinda CSS tokens (cream, sand, sage, terracotta).
 *
 * [line] is for dividers and decorative borders (low contrast on purpose); [lineStrong] is for
 * boundaries of interactive components (text fields, unselected chips, switch borders) and
 * meets the 3:1 of WCAG 1.4.11. [emphasis] is the brightest text (clock) and may exceed the
 * normal [text] contrast; running text uses [text].
 */
@Immutable
data class HushColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val emphasis: Color,
    val muted: Color,
    val accent: Color,
    val accentText: Color,
    val accentSoft: Color,
    val danger: Color,
    val success: Color,
    val bubble: Color,
    val bubbleText: Color,
    val isDark: Boolean,
    val isMinimal: Boolean,
)

object HushPalettes {
    /** Minimal Mode: black, white, one grey. Running text is #EDEDED to soften halation on OLED. */
    val Minimal = HushColors(
        background = Color(0xFF000000),
        surface = Color(0xFF0E0E0E),
        surfaceVariant = Color(0xFF1A1A1A),
        line = Color(0xFF2A2A2A),
        lineStrong = Color(0xFF5A5A5A),
        text = Color(0xFFEDEDED),
        emphasis = Color(0xFFFFFFFF),
        muted = Color(0xFF9A9A9A),
        accent = Color(0xFFFFFFFF),
        accentText = Color(0xFF000000),
        accentSoft = Color(0xFF262626),
        danger = Color(0xFFE0E0E0),
        success = Color(0xFFCFCFCF),
        bubble = Color(0xFF161616),
        bubbleText = Color(0xFFEDEDED),
        isDark = true,
        isMinimal = true,
    )

    /** Cozy Mode, light. From styles.css `:root` in mr-nook and melinda. */
    val CozyLight = HushColors(
        background = Color(0xFFFCF3E3),
        surface = Color(0xFFFDF8EC),
        surfaceVariant = Color(0xFFF3E6CF),
        line = Color(0xFFE9DBC2),
        lineStrong = Color(0xFF9C8467),
        text = Color(0xFF482D1E),
        emphasis = Color(0xFF482D1E),
        muted = Color(0xFF7D6350),
        accent = Color(0xFF6B6E4A),
        accentText = Color(0xFFFDF8EC),
        accentSoft = Color(0xFFDFE1CB),
        danger = Color(0xFFA84A2C),
        success = Color(0xFF5F6340),
        bubble = Color(0xFFFDF8EC),
        bubbleText = Color(0xFF482D1E),
        isDark = false,
        isMinimal = false,
    )

    /** Cozy Mode, dark. From styles.css `:root[data-theme="dark"]`. */
    val CozyDark = HushColors(
        background = Color(0xFF2A1D15),
        surface = Color(0xFF35271E),
        surfaceVariant = Color(0xFF443327),
        line = Color(0xFF503C2E),
        lineStrong = Color(0xFF8C735C),
        text = Color(0xFFFCF3E3),
        emphasis = Color(0xFFFCF3E3),
        muted = Color(0xFFC3AC94),
        accent = Color(0xFF9DA275),
        accentText = Color(0xFF2A1D15),
        accentSoft = Color(0xFF4A4D36),
        danger = Color(0xFFE07E5E),
        success = Color(0xFFB9BE8E),
        bubble = Color(0xFF35271E),
        bubbleText = Color(0xFFFCF3E3),
        isDark = true,
        isMinimal = false,
    )

    /** Extra brand colours used by scenes and the mascot bubble accents. */
    val Terracotta = Color(0xFFCE6946)
    val Peach = Color(0xFFF4B090)
    val Sand = Color(0xFFD4A576)
    val Sage = Color(0xFF82855B)
    val Brown = Color(0xFF6C4530)
    val Honey = Color(0xFFF2C14E)
}

val LocalHushColors = staticCompositionLocalOf { HushPalettes.Minimal }
