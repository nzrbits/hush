package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 13x13 pixel gear. '#' is a lit pixel. */
private val GEAR = listOf(
    "....##.##....",
    "...#######...",
    "..#########..",
    ".####...####.",
    "####.....####",
    "###.......###",
    ".##.......##.",
    "###.......###",
    "####.....####",
    ".####...####.",
    "..#########..",
    "...#######...",
    "....##.##....",
)

/** Draws a pixel pattern at [size] with crisp cells. */
@Composable
fun PixelGlyph(rows: List<String>, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val cols = rows.first().length
        val cell = this.size.width / cols
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                if (c == '#') drawRect(color, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

/** Retro gear button, 48 dp touch target, 24 dp glyph. */
@Composable
fun PixelGearButton(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = "Einstellungen" },
        contentAlignment = Alignment.Center,
    ) {
        PixelGlyph(rows = GEAR, color = color, size = 26.dp)
    }
}
