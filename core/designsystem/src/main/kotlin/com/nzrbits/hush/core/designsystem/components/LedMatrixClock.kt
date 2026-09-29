package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/**
 * LED matrix clock in the style of a desk pixel clock: a dark panel, a grid of small square
 * LEDs with visible gaps, unlit LEDs drawn faintly, a pixel calendar icon on the left and
 * the time in 5x7 dot digits. Static, no blinking: the launcher stays calm.
 *
 * Grid: 9 rows x 38 columns. Icon at columns 1..8, digits from column 12.
 */
@Composable
fun LedMatrixClock(
    digits: String,
    dayOfMonth: Int,
    modifier: Modifier = Modifier,
) {
    val colors = HushTheme.colors
    val minimal = colors.isMinimal
    val panel = if (minimal) Color(0xFF080808) else Color(0xFF241610)
    val frame = if (minimal) Color(0xFF262626) else Color(0xFF3B2519)
    val off = if (minimal) Color(0xFF181818) else Color(0xFF32211A)
    val led = if (minimal) Color(0xFFF2F2F2) else Color(0xFFF2C14E)
    val ledDim = if (minimal) Color(0xFFB8B8B8) else Color(0xFFD9A63E)
    val iconTop = if (minimal) Color(0xFFF2F2F2) else Color(0xFFCE6946)
    val iconBody = if (minimal) Color(0xFF9A9A9A) else Color(0xFFFCF3E3)
    val shape = RoundedCornerShape(if (minimal) 6.dp else 16.dp)

    val cols = 38
    val rows = 9
    val cells = buildMatrix(digits, dayOfMonth, cols, rows)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat() * 0.92f)
            .clip(shape)
            .background(panel)
            .border(2.dp, frame, shape)
            .padding(10.dp)
            .semantics { contentDescription = digits },
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(cols.toFloat() / rows.toFloat())) {
            val cell = size.width / cols
            val dot = cell * 0.74f
            val inset = (cell - dot) / 2f
            val radius = CornerRadius(dot * 0.18f)
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val kind = cells[y][x]
                    val color = when (kind) {
                        Cell.OFF -> off
                        Cell.LED -> led
                        Cell.LED_DIM -> ledDim
                        Cell.ICON_TOP -> iconTop
                        Cell.ICON_BODY -> iconBody
                    }
                    drawRoundRect(color, topLeft = Offset(x * cell + inset, y * cell + inset), size = Size(dot, dot), cornerRadius = radius)
                }
            }
        }
    }
}

private enum class Cell { OFF, LED, LED_DIM, ICON_TOP, ICON_BODY }

/** 5x7 dot digits, '#' lit. */
private val DIGITS: Map<Char, List<String>> = mapOf(
    '0' to listOf(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
    '1' to listOf("..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."),
    '2' to listOf(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
    '3' to listOf("#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."),
    '4' to listOf("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
    '5' to listOf("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
    '6' to listOf("..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."),
    '7' to listOf("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
    '8' to listOf(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
    '9' to listOf(".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."),
)

/** 8x8 calendar: two header rows, a body with the day number in a 3x5 face when it fits, else a grid. */
private fun calendarIcon(day: Int): List<String> {
    val body = if (day in 1..31) dayGlyph(day) else listOf("#.#.#.#.", "........", "#.#.#.#.", "........", "#.#.#.#.", "........")
    return listOf("TTTTTTTT", "TTTTTTTT") + body
}

private val SMALL: Map<Char, List<String>> = mapOf(
    '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
    '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
    '2' to listOf("###", "..#", "###", "#..", "###"),
    '3' to listOf("###", "..#", "###", "..#", "###"),
    '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
    '5' to listOf("###", "#..", "###", "..#", "###"),
    '6' to listOf("###", "#..", "###", "#.#", "###"),
    '7' to listOf("###", "..#", "..#", "..#", "..#"),
    '8' to listOf("###", "#.#", "###", "#.#", "###"),
    '9' to listOf("###", "#.#", "###", "..#", "###"),
)

/** Six body rows: one blank, five with the day number (two 3x5 digits with a 1-column gap), centred in 8 columns. */
private fun dayGlyph(day: Int): List<String> {
    val text = day.toString().padStart(2, '0')
    val a = SMALL.getValue(text[0])
    val b = SMALL.getValue(text[1])
    val rows = (0 until 5).map { r -> ".${a[r]}.${b[r]}." }
    return listOf("........") + rows
}

private fun buildMatrix(digits: String, day: Int, cols: Int, rows: Int): Array<Array<Cell>> {
    val m = Array(rows) { Array(cols) { Cell.OFF } }
    // Calendar icon: columns 1..8, rows 0..7.
    calendarIcon(day).forEachIndexed { y, row ->
        row.forEachIndexed { x, c ->
            val cell = when (c) { 'T' -> Cell.ICON_TOP; '#' -> Cell.ICON_BODY; else -> null }
            if (cell != null && y < rows && x + 1 < cols) m[y][x + 1] = cell
        }
    }
    // Time: right-aligned block of up to "HH:MM", digits 5 wide, colon 1 wide, 1-column gaps, rows 1..7.
    var x = 12
    val top = 1
    digits.forEach { ch ->
        if (ch == ':') {
            if (x < cols) { m[top + 2][x] = Cell.LED_DIM; m[top + 4][x] = Cell.LED_DIM }
            x += 2
        } else {
            val glyph = DIGITS[ch]
            if (glyph != null) {
                glyph.forEachIndexed { y, row -> row.forEachIndexed { dx, c -> if (c == '#' && x + dx < cols) m[top + y][x + dx] = Cell.LED } }
            }
            x += 6
        }
    }
    return m
}
