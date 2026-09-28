package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nzrbits.hush.core.designsystem.theme.HushTheme

@Composable
fun HushDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = HushTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(HushTheme.shapes.card),
            color = colors.surface,
            contentColor = colors.text,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = HushTheme.typography.title, color = colors.text)
                HushSpacer(12)
                content()
            }
        }
    }
}

@Composable
fun HushTextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    confirmLabel: String = "Speichern",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = HushTheme.colors
    var value by remember { mutableStateOf(initial) }
    HushDialog(title = title, onDismiss = onDismiss) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = { Text(placeholder, color = colors.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text,
                unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.line,
                cursorColor = colors.accent,
            ),
        )
        HushSpacer(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HushTextButton("Abbrechen", onClick = onDismiss)
            HushTextButton(confirmLabel, onClick = { onConfirm(value) })
        }
    }
}

@Composable
fun HushConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = false,
) {
    val colors = HushTheme.colors
    HushDialog(title = title, onDismiss = onDismiss) {
        Text(text, style = HushTheme.typography.body, color = colors.muted)
        HushSpacer(12)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            HushTextButton("Abbrechen", onClick = onDismiss)
            HushTextButton(confirmLabel, onClick = onConfirm, danger = danger)
        }
    }
}
