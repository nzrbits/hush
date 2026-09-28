package com.nzrbits.hush.feature.cozy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import kotlinx.coroutines.delay

/**
 * Mr. Nook's animation states. Sheet layout and timing match `scripts/mascot.mjs` and
 * `.nook-sprite` in the Mr. Nook web app: horizontal strips of 256 px frames, frame 0 is the
 * resting face, so a paused animation always shows him looking at you.
 */
enum class MascotState(val frames: Int, val cycleMillis: Int, val loops: Int) {
    IDLE(frames = 8, cycleMillis = 2400, loops = 0),
    TALK(frames = 7, cycleMillis = 1050, loops = 3),
    CHEER(frames = 8, cycleMillis = 1100, loops = 3),
    SLEEP(frames = 12, cycleMillis = 3600, loops = 0),
}

private const val FRAME_PX = 256

@Composable
private fun sheetFor(state: MascotState): ImageBitmap = when (state) {
    MascotState.IDLE -> ImageBitmap.imageResource(R.drawable.nook_idle)
    MascotState.TALK -> ImageBitmap.imageResource(R.drawable.nook_talk)
    MascotState.CHEER -> ImageBitmap.imageResource(R.drawable.nook_cheer)
    MascotState.SLEEP -> ImageBitmap.imageResource(R.drawable.nook_sleep)
}

/**
 * Draws Mr. Nook at [size]. Finite states (TALK, CHEER) play their loops and then fall back
 * to the resting frame. With reduced motion only frame 0 is shown.
 */
@Composable
fun NookMascot(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val reduceMotion = HushTheme.reduceMotion
    val sheet = sheetFor(state)
    var frame by remember(state) { mutableIntStateOf(0) }
    LocalContext.current

    LaunchedEffect(state, reduceMotion) {
        frame = 0
        if (reduceMotion) return@LaunchedEffect
        val frameMillis = (state.cycleMillis / state.frames).toLong()
        var played = 0
        while (true) {
            for (i in 0 until state.frames) {
                frame = i
                delay(frameMillis)
            }
            played++
            if (state.loops > 0 && played >= state.loops) {
                frame = 0
                return@LaunchedEffect
            }
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = HushConfig.MASCOT_NAME },
    ) {
        val dst = IntSize(this.size.width.toInt(), this.size.height.toInt())
        drawImage(
            image = sheet,
            srcOffset = IntOffset(frame * FRAME_PX, 0),
            srcSize = IntSize(FRAME_PX, FRAME_PX),
            dstOffset = IntOffset.Zero,
            dstSize = dst,
            filterQuality = FilterQuality.None,
        )
    }
}
