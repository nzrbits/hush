package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/** A plain settings row: title, optional subtitle, optional trailing text. */
@Composable
fun HushRow(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = HushTheme.colors
    val base = Modifier
        .fillMaxWidth()
        .let {
            if (onClick != null || onLongClick != null) {
                it.combinedClickable(enabled = enabled, onClick = { onClick?.invoke() }, onLongClick = onLongClick)
            } else it
        }
        .padding(vertical = 14.dp)
    Row(modifier = base, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = HushTheme.typography.body, color = if (enabled) colors.text else colors.muted)
            if (subtitle != null) {
                Text(subtitle, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (trailing != null) {
            Text(trailing, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(start = 12.dp))
        }
        if (trailingContent != null) {
            Box(Modifier.padding(start = 12.dp)) { trailingContent() }
        }
    }
}

@Composable
fun HushSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = HushTheme.colors
    HushRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.accentText,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.muted,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedBorderColor = colors.line,
                ),
            )
        },
    )
}

/** Card surface. In Minimal Mode it is a flat block separated by lines; in Cozy it is a rounded card. */
@Composable
fun HushCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = HushTheme.colors
    val shape = RoundedCornerShape(HushTheme.shapes.card)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceVariant)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
    ) { content() }
}

/** Large text list item used for app names on the home screen and in the drawer. */
@Composable
fun HushAppItem(
    label: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    dimmed: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = HushTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = HushTheme.typography.listItem,
            color = if (dimmed) colors.muted else colors.text,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (secondary != null) {
            Text(secondary, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
fun HushEmptyState(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = HushTheme.typography.body,
        color = HushTheme.colors.muted,
        modifier = modifier.padding(vertical = 24.dp),
    )
}
