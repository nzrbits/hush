package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HushPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = HushTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HushTheme.shapes.small),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.accentText,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.muted,
        ),
    ) { Text(text, style = HushTheme.typography.body, modifier = Modifier.padding(vertical = 4.dp)) }
}

@Composable
fun HushSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = HushTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HushTheme.shapes.small),
        border = BorderStroke(1.dp, colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text, disabledContentColor = colors.muted),
    ) { Text(text, style = HushTheme.typography.body, modifier = Modifier.padding(vertical = 4.dp)) }
}

/** Text button without Material's 12 dp side inset, so it aligns with the text column it sits in. */
@Composable
fun HushTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val colors = HushTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = if (danger) colors.danger else colors.text),
    ) { Text(text, style = HushTheme.typography.body) }
}

/** Horizontal choice chips, one selected. Used for enum settings. */
@Composable
fun <T> HushChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = HushTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            OutlinedButton(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(HushTheme.shapes.pill.coerceAtMost(20.dp)),
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.lineStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) colors.accent else colors.surface,
                    contentColor = if (isSelected) colors.accentText else colors.text,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) { Text(label(option), style = HushTheme.typography.caption, maxLines = 1) }
        }
    }
}

/**
 * Seven weekday toggles as 40 dp circles with a 4 dp gap (304 dp total, fits every phone).
 * Selected = accent fill, unselected = surface with a strong border. No glyph prefixes.
 */
@Composable
fun HushDayToggleRow(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    val colors = HushTheme.colors
    val shape = if (HushTheme.colors.isMinimal) RoundedCornerShape(4.dp) else CircleShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayOfWeek.entries.forEach { day ->
            val on = day in selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(shape)
                    .background(if (on) colors.accent else colors.surface)
                    .border(1.dp, if (on) colors.accent else colors.lineStrong, shape)
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2),
                    style = HushTheme.typography.caption,
                    color = if (on) colors.accentText else colors.text,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
