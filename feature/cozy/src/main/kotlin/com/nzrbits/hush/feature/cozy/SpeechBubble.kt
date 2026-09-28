package com.nzrbits.hush.feature.cozy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/**
 * Mr. Nook next to a speech bubble. Used on the Cozy home screen, on the blocked screen and
 * in settings. Two sizes only: 84 dp on the home screen, 64 dp everywhere else.
 */
@Composable
fun MascotBubble(
    text: String,
    state: MascotState,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 84.dp,
    onTap: (() -> Unit)? = null,
) {
    val colors = HushTheme.colors
    val shape = RoundedCornerShape(HushTheme.shapes.card.coerceAtLeast(6.dp))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onTap != null) it.clickable(onClick = onTap) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NookMascot(state = state, size = mascotSize)
        Box(modifier = Modifier.padding(start = 4.dp)) {
            // Tail: a small triangle that overlaps the bubble by 1 dp; only its two outer edges are stroked.
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 9.dp, height = 12.dp),
            ) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w + 2f, 0f)
                    lineTo(0f, h / 2f)
                    lineTo(w + 2f, h)
                    close()
                }
                drawPath(path, colors.bubble)
                val edge = Path().apply {
                    moveTo(w + 2f, 0f)
                    lineTo(0f, h / 2f)
                    lineTo(w + 2f, h)
                }
                drawPath(edge, colors.line, style = Stroke(width = 1.dp.toPx()))
            }
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(shape)
                    .background(colors.bubble)
                    .border(1.dp, colors.line, shape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text = text, style = HushTheme.typography.bubble, color = colors.bubbleText)
            }
        }
    }
}

@Suppress("unused") private val keepOffset = Offset.Zero
