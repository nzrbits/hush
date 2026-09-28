package com.nzrbits.hush.feature.cozy

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/**
 * Mr. Nook next to a speech bubble. Used on the Cozy home screen, on the blocked screen and
 * in settings. The bubble is a plain rounded box with a small square "tail" so it stays pixel-ish.
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
        Box(modifier = Modifier.padding(start = 6.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp)
                    .size(10.dp)
                    .rotate(45f)
                    .background(colors.bubble)
                    .border(1.dp, colors.line),
            )
            Box(
                modifier = Modifier
                    .padding(start = 5.dp)
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
