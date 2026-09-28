package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nzrbits.hush.core.common.time.Durations
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.roundToLong

/**
 * Slider over a duration in minutes with a logarithmic scale, so one hour and 30 days both
 * get usable resolution. Values snap to sensible steps (15 min under 3 h, 1 h under a day, then days).
 */
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
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surfaceVariant,
            ),
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
