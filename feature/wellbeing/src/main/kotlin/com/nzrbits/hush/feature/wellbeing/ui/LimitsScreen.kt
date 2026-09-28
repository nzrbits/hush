package com.nzrbits.hush.feature.wellbeing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushChoiceRow
import com.nzrbits.hush.core.designsystem.components.HushDialog
import com.nzrbits.hush.core.designsystem.components.HushEmptyState
import com.nzrbits.hush.core.designsystem.components.HushPrimaryButton
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushScreen
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.components.HushSpacer
import com.nzrbits.hush.core.designsystem.components.HushSwitchRow
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.theme.HushTheme

private val presets = listOf(15, 30, 45, 60, 90, 120)

/** Daily usage limits. These are reminders: a notification when the limit is reached, never a block. */
@Composable
fun LimitsScreen(onBack: () -> Unit, onOpenPermissions: () -> Unit, viewModel: LimitsViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val limits by viewModel.limits.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<String?>(null) }
    var choosing by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.preselected) { viewModel.preselected?.let { editing = it } }

    HushScreen(title = "Zeiterinnerungen", onBack = onBack) {
        HushSwitchRow(
            title = "Erinnerungen senden",
            subtitle = "Eine Benachrichtigung bei 80 % und eine beim Erreichen des Limits. Keine Sperre.",
            checked = remindersEnabled,
            onCheckedChange = viewModel::setRemindersEnabled,
        )
        if (!viewModel.hasUsageAccess) {
            HushSpacer(8)
            HushCard(onClick = onOpenPermissions) {
                Text("Nutzungszugriff fehlt. Ohne ihn kann ${HushConfig.APP_NAME} die Nutzungsdauer nicht messen.", style = HushTheme.typography.caption, color = colors.text)
            }
        }
        HushSectionHeader("Limits")
        HushPrimaryButton("App hinzufügen", onClick = { choosing = true })
        HushSpacer(4)
        if (limits.isEmpty()) HushEmptyState("Noch keine Limits. Zum Beispiel Instagram: 15 Minuten.")
        limits.forEach { limit ->
            HushRow(
                title = viewModel.labelFor(limit.packageName),
                subtitle = "${limit.dailyLimitMinutes} Minuten pro Tag",
                onClick = { editing = limit.packageName },
                trailingContent = { HushTextButton("Entfernen", onClick = { viewModel.remove(limit.packageName) }, danger = true) },
            )
        }
    }

    editing?.let { pkg ->
        val current = limits.firstOrNull { it.packageName == pkg }?.dailyLimitMinutes ?: 30
        var minutes by remember(pkg) { mutableStateOf(current) }
        HushDialog(title = viewModel.labelFor(pkg), onDismiss = { editing = null }) {
            Text("Tägliches Limit in Minuten", style = HushTheme.typography.caption, color = colors.muted)
            HushChoiceRow(options = presets.take(3), selected = minutes, label = { "$it" }, onSelect = { minutes = it })
            HushChoiceRow(options = presets.drop(3), selected = minutes, label = { "$it" }, onSelect = { minutes = it })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HushTextButton("Abbrechen", onClick = { editing = null })
                HushTextButton("Speichern", onClick = { viewModel.set(pkg, minutes); editing = null })
            }
        }
    }

    if (choosing) {
        AppMultiPickerDialog(
            apps = apps.map { it.packageName to it.displayLabel }.distinctBy { it.first },
            selected = limits.map { it.packageName }.toSet(),
            onToggle = { pkg, on -> if (on) { choosing = false; editing = pkg } else viewModel.remove(pkg) },
            onDismiss = { choosing = false },
        )
    }
}
