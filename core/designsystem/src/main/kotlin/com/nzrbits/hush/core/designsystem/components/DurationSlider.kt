package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.common.time.Durations
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToLong

/**
 * Slider over a duration in minutes with a logarithmic scale, so one hour and 30 days both
 * get usable resolution. Values snap to sensible steps (15 min under 3 h, 1 h under a day, then days).
 * The thumb is a 24 dp disc, not Material's thin bar, so it does not read as a text cursor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSlider(
    minutes: Long,
    minMinutes: Long,
    maxMinutes: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HushTheme.colors
    val lnMin = ln(minMinutes.toDouble())
    val lnMax = ln(maxMinutes.toDouble())
    val position = ((ln(minutes.coerceIn(minMinutes, maxMinutes).toDouble()) - lnMin) / (lnMax - lnMin)).toFloat()
    val thumbShape = if (colors.isMinimal) RoundedCornerShape(4.dp) else CircleShape
    Column(modifier.fillMaxWidth()) {
        Text(
            text = Durations.formatMinutes(minutes),
            style = HushTheme.typography.title,
            color = colors.text,
        )
        Slider(
            value = position,
            onValueChange = { p ->
                val raw = exp(lnMin + (lnMax - lnMin) * p.toDouble()).roundToLong()
                onChange(snapDuration(raw, minMinutes, maxMinutes))
            },
            thumb = {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(thumbShape)
                        .background(colors.accent),
                )
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = SliderDefaults.colors(
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = if (colors.isMinimal) colors.lineStrong else colors.accentSoft,
                        activeTickColor = colors.accent,
                        inactiveTickColor = colors.lineStrong,
                    ),
                    drawStopIndicator = null,
                    thumbTrackGapSize = 4.dp,
                )
            },
        )
    }
}

fun snapDuration(rawMinutes: Long, min: Long, max: Long): Long {
    val step = when {
        rawMinutes < 3 * 60 -> 15L
        rawMinutes < 24 * 60 -> 60L
        rawMinutes < 3 * 24 * 60 -> 6 * 60L
        else -> 24 * 60L
    }
    val snapped = ((rawMinutes + step / 2) / step) * step
    return snapped.coerceIn(min, max)
}
