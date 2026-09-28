package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

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
        border = BorderStroke(1.dp, colors.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text, disabledContentColor = colors.muted),
    ) { Text(text, style = HushTheme.typography.body, modifier = Modifier.padding(vertical = 4.dp)) }
}

@Composable
fun HushTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val colors = HushTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier,
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
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.line),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) colors.accent else colors.surface,
                    contentColor = if (isSelected) colors.accentText else colors.text,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) { Text(label(option), style = HushTheme.typography.caption, maxLines = 1) }
        }
    }
}
