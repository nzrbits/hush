package com.nzrbits.hush.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import kotlin.math.roundToInt

/**
 * Slide-to-confirm control. The user drags the knob to the right end to trigger [onConfirmed].
 * Accessibility services can activate it with a plain click action.
 */
@Composable
fun SlideToConfirm(
    text: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = HushTheme.colors
    val density = LocalDensity.current
    val knobSize = 52.dp
    val knobPx = with(density) { knobSize.toPx() }
    var trackWidth by remember { mutableIntStateOf(0) }
    var offset by remember { mutableFloatStateOf(0f) }
    val maxOffset = (trackWidth - knobPx).coerceAtLeast(0f)
    val animated by animateFloatAsState(targetValue = offset, label = "knob")
    val progress = if (maxOffset > 0) (animated / maxOffset).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(knobSize + 8.dp)
            .clip(RoundedCornerShape(HushTheme.shapes.pill.coerceAtMost(30.dp)))
            .background(colors.surfaceVariant)
            .onSizeChanged { trackWidth = it.width }
            .semantics {
                role = Role.Button
                contentDescription = text
                onClick { if (enabled) { onConfirmed(); true } else false }
            }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = HushTheme.typography.body,
            color = colors.muted.copy(alpha = 1f - progress * 0.8f),
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(animated.roundToInt(), 0) }
                .size(knobSize)
                .clip(RoundedCornerShape(HushTheme.shapes.pill.coerceAtMost(26.dp)))
                .background(if (enabled) colors.accent else colors.line)
                .pointerInput(enabled, maxOffset) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offset >= maxOffset * 0.9f) {
                                onConfirmed()
                            }
                            offset = 0f
                        },
                        onDragCancel = { offset = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offset = (offset + dragAmount).coerceIn(0f, maxOffset)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("›", style = HushTheme.typography.title, color = colors.accentText)
        }
    }
}
