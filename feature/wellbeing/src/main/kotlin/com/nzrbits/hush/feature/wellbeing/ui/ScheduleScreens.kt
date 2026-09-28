package com.nzrbits.hush.feature.wellbeing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.model.BlockSchedule
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
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private fun LocalTime.hm(): String = String.format(Locale.GERMAN, "%02d:%02d", hour, minute)

fun BlockSchedule.daysLabel(): String {
    val all = DayOfWeek.entries
    if (days.size == 7) return "Täglich"
    if (days == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) return "Montag bis Freitag"
    if (days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) return "Wochenende"
    return all.filter { it in days }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.GERMAN) }
}

@Composable
fun SchedulesScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, viewModel: SchedulesViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val activeIds = status.scheduled.map { it.first.id }.toSet()

    HushScreen(title = "Blockierpläne", onBack = onBack) {
        HushPrimaryButton("Neuer Plan", onClick = { onEdit(0L) })
        HushSpacer(8)
        if (schedules.isEmpty()) HushEmptyState("Noch kein Plan. Zum Beispiel: Fokuszeit, Montag bis Freitag, 09:00 bis 12:00.")
        schedules.forEach { s ->
            HushRow(
                title = s.name + if (s.id in activeIds) " · läuft" else "",
                subtitle = "${s.daysLabel()} · ${s.startTime.hm()}–${s.endTime.hm()}" + (if (s.crossesMidnight) " (über Nacht)" else "") +
                    "\n" + s.packageNames.joinToString(", ") { viewModel.labelFor(it) }.ifEmpty { "Keine Apps gewählt" },
                onClick = { onEdit(s.id) },
                trailingContent = {
                    androidx.compose.material3.Switch(
                        checked = s.enabled,
                        onCheckedChange = { viewModel.setEnabled(s.id, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.accentText),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(onBack: () -> Unit, viewModel: ScheduleEditViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = start, false = end
    var choosingApps by remember { mutableStateOf(false) }

    HushScreen(title = if (viewModel.isNew) "Neuer Plan" else "Plan bearbeiten", onBack = onBack) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = { Text("Name", color = colors.muted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.text, unfocusedTextColor = colors.text,
                focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line, cursorColor = colors.accent,
            ),
        )
        HushSectionHeader("Wochentage")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day in draft.days
                HushTextButton(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2),
                    onClick = { viewModel.update { it.copy(days = if (on) it.days - day else it.days + day) } },
                    modifier = Modifier.weight(1f),
                    danger = false,
                )
            }
        }
        Text(draft.daysLabel(), style = HushTheme.typography.caption, color = colors.muted)
        HushChoiceRow(
            options = listOf("Mo–Fr", "Wochenende", "Täglich"),
            selected = when (draft.daysLabel()) { "Montag bis Freitag" -> "Mo–Fr"; "Wochenende" -> "Wochenende"; "Täglich" -> "Täglich"; else -> "" },
            label = { it },
            onSelect = { choice ->
                viewModel.update {
                    it.copy(
                        days = when (choice) {
                            "Mo–Fr" -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                            "Wochenende" -> setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                            else -> DayOfWeek.entries.toSet()
                        },
                    )
                }
            },
        )

        HushSectionHeader("Zeit")
        HushRow("Start", trailing = draft.startTime.hm(), onClick = { picking = true })
        HushRow("Ende", trailing = draft.endTime.hm() + if (draft.crossesMidnight) " (nächster Tag)" else "", onClick = { picking = false })

        HushSectionHeader("Apps")
        HushRow(
            title = if (draft.packageNames.isEmpty()) "Apps wählen" else draft.packageNames.joinToString(", ") { pkg -> apps.firstOrNull { it.packageName == pkg }?.displayLabel ?: pkg },
            subtitle = "${draft.packageNames.size} gewählt",
            onClick = { choosingApps = true },
        )

        HushSpacer(24)
        HushPrimaryButton("Speichern", onClick = { viewModel.save(onBack) }, enabled = draft.packageNames.isNotEmpty() && draft.days.isNotEmpty())
        if (!viewModel.isNew) {
            HushSpacer(8)
            HushTextButton("Plan löschen", onClick = { viewModel.delete(onBack) }, danger = true)
        }
    }

    picking?.let { isStart ->
        val initial = if (isStart) draft.startTime else draft.endTime
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        HushDialog(title = if (isStart) "Startzeit" else "Endzeit", onDismiss = { picking = null }) {
            TimePicker(state = state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HushTextButton("Abbrechen", onClick = { picking = null })
                HushTextButton("Übernehmen", onClick = {
                    val t = LocalTime.of(state.hour, state.minute)
                    viewModel.update { if (isStart) it.copy(startTime = t) else it.copy(endTime = t) }
                    picking = null
                })
            }
        }
    }

    if (choosingApps) {
        AppMultiPickerDialog(
            apps = apps.map { it.packageName to it.displayLabel }.distinctBy { it.first },
            selected = draft.packageNames,
            onToggle = { pkg, on -> viewModel.update { it.copy(packageNames = if (on) it.packageNames + pkg else it.packageNames - pkg) } },
            onDismiss = { choosingApps = false },
        )
    }
}

@Composable
fun AppMultiPickerDialog(
    apps: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = HushTheme.colors
    var filter by remember { mutableStateOf("") }
    HushDialog(title = "Apps", onDismiss = onDismiss) {
        OutlinedTextField(
            value = filter, onValueChange = { filter = it }, singleLine = true,
            placeholder = { Text("Filtern", color = colors.muted) }, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.text, unfocusedTextColor = colors.text, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            apps.filter { filter.isBlank() || it.second.contains(filter, ignoreCase = true) }.forEach { (pkg, label) ->
                val on = pkg in selected
                HushRow(
                    title = label,
                    onClick = { onToggle(pkg, !on) },
                    trailingContent = {
                        Checkbox(checked = on, onCheckedChange = { onToggle(pkg, it) }, colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.accentText))
                    },
                )
            }
        }
        HushSpacer(8)
        HushPrimaryButton("Fertig", onClick = onDismiss)
    }
}
