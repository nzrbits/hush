package com.nzrbits.hush.feature.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.CapturedNotification
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.model.NotificationRuleMode
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushChoiceRow
import com.nzrbits.hush.core.designsystem.components.HushConfirmDialog
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
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.permissions.PermissionsChecker
import com.nzrbits.hush.feature.notifications.data.NotificationRepository
import com.nzrbits.hush.feature.wellbeing.ui.AppMultiPickerDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val stampFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd.MM. HH:mm", Locale.GERMAN)
private fun LocalTime.hm() = String.format(Locale.GERMAN, "%02d:%02d", hour, minute)

@HiltViewModel
class NotificationLogViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val settings: SettingsRepository,
    private val permissions: PermissionsChecker,
) : ViewModel() {
    val log: StateFlow<List<CapturedNotification>> = repository.log.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val enabled = settings.settings.map { it.wellbeing.notificationFilterEnabled }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val hasAccess: Boolean get() = permissions.hasNotificationAccess()
    fun delete(id: Long) = viewModelScope.launch { repository.deleteEntry(id) }
    fun clear() = viewModelScope.launch { repository.clearLog() }
    fun setEnabled(on: Boolean) = viewModelScope.launch { settings.updateWellbeing { it.copy(notificationFilterEnabled = on) } }
}

@HiltViewModel
class NotificationRulesViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val apps: AppsRepository,
) : ViewModel() {
    val rules: StateFlow<List<NotificationRule>> = repository.rules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setEnabled(id: Long, on: Boolean) = viewModelScope.launch { repository.setEnabled(id, on) }
    fun labelFor(pkg: String) = apps.labelFor(pkg)
}

@HiltViewModel
class NotificationRuleEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: NotificationRepository,
    appsRepository: AppsRepository,
) : ViewModel() {
    private val id: Long = savedState.get<String>("id")?.toLongOrNull() ?: 0L
    val isNew get() = id == 0L
    val apps: StateFlow<List<LauncherApp>> = appsRepository.visibleApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val draft = MutableStateFlow(NotificationRule(id = 0, name = "", enabled = true, mode = NotificationRuleMode.BLOCKLIST, packageNames = emptySet()))

    init { if (id != 0L) viewModelScope.launch { repository.rule(id)?.let { draft.value = it } } }

    fun update(t: (NotificationRule) -> NotificationRule) { draft.value = t(draft.value) }
    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = draft.value
        repository.save(d.copy(name = d.name.ifBlank { "Filter" }))
        onDone()
    }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { if (id != 0L) repository.delete(id); onDone() }
}

@Composable
fun NotificationLogScreen(onBack: () -> Unit, onOpenRules: () -> Unit, onOpenPermissions: () -> Unit, viewModel: NotificationLogViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val log by viewModel.log.collectAsStateWithLifecycle()
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    HushScreen(
        title = "Gefilterte Meldungen",
        onBack = onBack,
        actions = { HushTextButton("Regeln", onClick = onOpenRules) },
    ) {
        HushSwitchRow(
            title = "Filter aktiv",
            subtitle = "Passende Benachrichtigungen landen hier statt in der Leiste.",
            checked = enabled,
            onCheckedChange = viewModel::setEnabled,
        )
        if (!viewModel.hasAccess) {
            HushCard(onClick = onOpenPermissions) {
                Column {
                    Text("Benachrichtigungszugriff fehlt", style = HushTheme.typography.body, color = colors.text)
                    Text(
                        "${HushConfig.APP_NAME} liest dafür Titel und Text eingehender Meldungen. Sie werden nur auf dem Gerät gespeichert und nach 30 Tagen gelöscht. " +
                            "Android erlaubt das Entfernen erst nach dem Eintreffen, ein Ton kann also schon gespielt haben.",
                        style = HushTheme.typography.caption, color = colors.muted,
                    )
                    HushSpacer(8)
                    HushPrimaryButton("Zugriff erlauben", onClick = onOpenPermissions)
                }
            }
        }
        HushSectionHeader("Chronologisch")
        if (log.isEmpty()) HushEmptyState("Noch nichts gefiltert.")
        else HushTextButton("Alle löschen", onClick = { confirmClear = true }, danger = true)
        log.forEach { n ->
            HushRow(
                title = "${n.appLabel} · ${n.title}".trimEnd(' ', '·'),
                subtitle = n.text + "\n" + Instant.ofEpochMilli(n.postedAtMillis).atZone(ZoneId.systemDefault()).format(stampFmt),
                trailingContent = { HushTextButton("×", onClick = { viewModel.delete(n.id) }) },
            )
        }
    }
    if (confirmClear) {
        HushConfirmDialog(
            title = "Alle löschen?", text = "Die gespeicherten Meldungen werden entfernt.", confirmLabel = "Löschen", danger = true,
            onDismiss = { confirmClear = false }, onConfirm = { viewModel.clear(); confirmClear = false },
        )
    }
}

@Composable
fun NotificationRulesScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, viewModel: NotificationRulesViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    HushScreen(title = "Filterregeln", onBack = onBack) {
        HushPrimaryButton("Neue Regel", onClick = { onEdit(0L) })
        HushSpacer(8)
        if (rules.isEmpty()) HushEmptyState("Noch keine Regel. Beispiel: Nachts 22:00 bis 07:00 alles außer Telefon und Nachrichten.")
        rules.forEach { r ->
            val window = if (r.windowStart != null && r.windowEnd != null) "${r.windowStart!!.hm()}–${r.windowEnd!!.hm()}" else "immer"
            val mode = if (r.mode == NotificationRuleMode.BLOCKLIST) "Blockiert" else "Erlaubt nur"
            HushRow(
                title = r.name,
                subtitle = "$window · $mode: " + r.packageNames.joinToString(", ") { viewModel.labelFor(it) }.ifEmpty { "keine Apps" },
                onClick = { onEdit(r.id) },
                trailingContent = {
                    androidx.compose.material3.Switch(
                        checked = r.enabled, onCheckedChange = { viewModel.setEnabled(r.id, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.accentText),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationRuleEditScreen(onBack: () -> Unit, viewModel: NotificationRuleEditViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<Boolean?>(null) }
    var choosing by remember { mutableStateOf(false) }

    HushScreen(title = if (viewModel.isNew) "Neue Regel" else "Regel bearbeiten", onBack = onBack) {
        OutlinedTextField(
            value = draft.name, onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = { Text("Name", color = colors.muted) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.text, unfocusedTextColor = colors.text, focusedBorderColor = colors.accent, unfocusedBorderColor = colors.line, cursorColor = colors.accent),
        )
        HushSectionHeader("Modus")
        HushChoiceRow(
            options = NotificationRuleMode.entries,
            selected = draft.mode,
            label = { if (it == NotificationRuleMode.BLOCKLIST) "Gelistete blockieren" else "Nur gelistete erlauben" },
            onSelect = { m -> viewModel.update { it.copy(mode = m) } },
        )
        HushSectionHeader("Apps")
        HushRow(
            title = if (draft.packageNames.isEmpty()) "Apps wählen" else draft.packageNames.joinToString(", ") { pkg -> apps.firstOrNull { it.packageName == pkg }?.displayLabel ?: pkg },
            subtitle = "${draft.packageNames.size} gewählt",
            onClick = { choosing = true },
        )
        HushSectionHeader("Zeitfenster")
        HushSwitchRow(
            title = "Nur in einem Zeitfenster",
            checked = draft.windowStart != null,
            onCheckedChange = { on -> viewModel.update { if (on) it.copy(windowStart = LocalTime.of(22, 0), windowEnd = LocalTime.of(7, 0)) else it.copy(windowStart = null, windowEnd = null) } },
        )
        if (draft.windowStart != null) {
            HushRow("Von", trailing = draft.windowStart!!.hm(), onClick = { picking = true })
            HushRow("Bis", trailing = draft.windowEnd!!.hm() + if (!draft.windowEnd!!.isAfter(draft.windowStart!!)) " (nächster Tag)" else "", onClick = { picking = false })
        }
        HushSectionHeader("Wochentage")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                val on = day in draft.days
                HushTextButton(
                    text = (if (on) "●" else "○") + day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.GERMAN).take(2),
                    onClick = { viewModel.update { it.copy(days = if (on) it.days - day else it.days + day) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HushSpacer(24)
        HushPrimaryButton("Speichern", onClick = { viewModel.save(onBack) }, enabled = draft.days.isNotEmpty())
        if (!viewModel.isNew) {
            HushSpacer(8)
            HushTextButton("Regel löschen", onClick = { viewModel.delete(onBack) }, danger = true)
        }
    }

    picking?.let { isStart ->
        val initial = (if (isStart) draft.windowStart else draft.windowEnd) ?: LocalTime.of(22, 0)
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
        HushDialog(title = if (isStart) "Von" else "Bis", onDismiss = { picking = null }) {
            TimePicker(state = state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                HushTextButton("Abbrechen", onClick = { picking = null })
                HushTextButton("Übernehmen", onClick = {
                    val t = LocalTime.of(state.hour, state.minute)
                    viewModel.update { if (isStart) it.copy(windowStart = t) else it.copy(windowEnd = t) }
                    picking = null
                })
            }
        }
    }
    if (choosing) {
        AppMultiPickerDialog(
            apps = apps.map { it.packageName to it.displayLabel }.distinctBy { it.first },
            selected = draft.packageNames,
            onToggle = { pkg, on -> viewModel.update { it.copy(packageNames = if (on) it.packageNames + pkg else it.packageNames - pkg) } },
            onDismiss = { choosing = false },
        )
    }
}
