package com.nzrbits.hush.feature.updates

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.BuildInfo
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.UpdateSettings
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushScreen
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.components.HushSpacer
import com.nzrbits.hush.core.designsystem.components.HushSwitchRow
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.feature.cozy.MascotBubble
import com.nzrbits.hush.feature.cozy.MascotState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<UpdateState> = repository.state
    val prefs: StateFlow<UpdateSettings> = settings.settings.map { it.updates }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpdateSettings())
    val currentVersion: String get() = repository.currentVersion

    /** App start: a throttled check, only when the user switched it on. */
    fun checkIfDue() = viewModelScope.launch { repository.check(force = false) }
    fun checkNow() = viewModelScope.launch { repository.check(force = true) }

    fun downloadAndInstall(release: ReleaseInfo) = viewModelScope.launch {
        val file = repository.download(release) ?: return@launch
        repository.install(release, file)
    }

    fun install(release: ReleaseInfo, file: java.io.File) = viewModelScope.launch { repository.install(release, file) }
    fun openInstallPermission() { runCatching { context.startActivity(repository.installPermissionIntent()) } }
    fun skip(version: String) = viewModelScope.launch { repository.skip(version) }
    fun dismissError() = repository.dismissError()
    fun setAutoCheck(on: Boolean) = viewModelScope.launch {
        settings.updateUpdates { it.copy(autoCheck = on) }
        if (on) repository.check(force = true)
    }
    fun setAutoInstall(on: Boolean) = viewModelScope.launch { settings.updateUpdates { it.copy(autoInstall = on) } }
}

/**
 * One line on the home screen, nothing else. Appears only while there is something to do.
 * In Cozy Mode Mr. Nook says it.
 */
@Composable
fun HomeUpdateLine(showMascot: Boolean, viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    LaunchedEffect(Unit) { viewModel.checkIfDue() }

    val pair: Pair<String, () -> Unit> = when (val s = state) {
        is UpdateState.Available -> "Update ${s.release.version} · Installieren" to { viewModel.downloadAndInstall(s.release); Unit }
        is UpdateState.Downloading -> "Update ${s.release.version} · ${(s.progress * 100).toInt()} %" to {}
        is UpdateState.ReadyToInstall -> "Update ${s.release.version} · Installieren" to { viewModel.install(s.release, s.file); Unit }
        is UpdateState.NeedsInstallPermission -> "Update ${s.release.version} · Installation erlauben" to { viewModel.openInstallPermission() }
        is UpdateState.Installing -> "Update ${s.release.version} · wird installiert" to {}
        is UpdateState.Failed -> "${s.message} · Ausblenden" to { viewModel.dismissError() }
        else -> return
    }
    val (text, action) = pair
    if (showMascot) {
        MascotBubble(text = text, state = MascotState.TALK, mascotSize = 64.dp, onTap = action, modifier = Modifier.padding(top = 12.dp))
    } else {
        Text(
            text = text,
            style = HushTheme.typography.body,
            color = colors.muted,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable(onClick = action),
        )
    }
}

@Composable
fun UpdateSettingsScreen(onBack: () -> Unit, viewModel: UpdateViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    HushScreen(title = "Updates", onBack = onBack) {
        HushRow("Installiert", trailing = viewModel.currentVersion)
        HushSwitchRow(
            title = "Automatisch nach Updates suchen",
            subtitle = "Alle sechs Stunden ein Abruf von github.com/nzrbits/hush. Das ist die einzige Netzverbindung von ${HushConfig.APP_NAME}, und nur mit diesem Schalter.",
            checked = prefs.autoCheck,
            onCheckedChange = viewModel::setAutoCheck,
        )
        HushSwitchRow(
            title = "Automatisch installieren",
            subtitle = "Lädt das Update im Hintergrund und übergibt es dem Installer. Ab Android 12 ohne Dialog, sobald ${HushConfig.APP_NAME} sein eigener Installer ist. Das erste Update fragt noch einmal.",
            checked = prefs.autoInstall,
            enabled = prefs.autoCheck,
            onCheckedChange = viewModel::setAutoInstall,
        )
        HushSpacer(8)
        HushTextButton("Jetzt prüfen", onClick = viewModel::checkNow)

        HushSectionHeader("Status")
        when (val s = state) {
            UpdateState.Idle -> Text("Noch nicht geprüft.", style = HushTheme.typography.body, color = colors.muted)
            UpdateState.Checking -> Text("Prüfe …", style = HushTheme.typography.body, color = colors.muted)
            UpdateState.UpToDate -> Text("${HushConfig.APP_NAME} ist aktuell.", style = HushTheme.typography.body, color = colors.muted)
            is UpdateState.Failed -> Text(s.message, style = HushTheme.typography.body, color = colors.danger)
            is UpdateState.Available -> ReleaseCard(s.release, "Herunterladen und installieren", { viewModel.downloadAndInstall(s.release) }, { viewModel.skip(s.release.version) })
            is UpdateState.Downloading -> Text("Lade ${s.release.version} · ${(s.progress * 100).toInt()} %", style = HushTheme.typography.body, color = colors.text)
            is UpdateState.ReadyToInstall -> ReleaseCard(s.release, "Installieren", { viewModel.install(s.release, s.file) }, { viewModel.skip(s.release.version) })
            is UpdateState.NeedsInstallPermission -> ReleaseCard(s.release, "Installation aus unbekannten Quellen erlauben", { viewModel.openInstallPermission() }, { viewModel.skip(s.release.version) })
            is UpdateState.Installing -> Text("Installiere ${s.release.version} …", style = HushTheme.typography.body, color = colors.text)
        }

        HushSectionHeader("So funktioniert es")
        Text(
            "Updates kommen als APK von den GitHub-Releases. Die Signatur muss zur installierten Version passen, sonst lehnt Android ab. " +
                "Beim Installieren wird ${HushConfig.APP_NAME} kurz beendet und von Android als Startbildschirm neu gestartet. Einstellungen und Favoriten bleiben erhalten.",
            style = HushTheme.typography.caption, color = colors.muted,
        )
        Text("Build ${BuildInfo.versionName} (${BuildInfo.versionCode})", style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ReleaseCard(release: ReleaseInfo, actionLabel: String, onAction: () -> Unit, onSkip: () -> Unit) {
    val colors = HushTheme.colors
    HushCard {
        Column {
            Text("Version ${release.version}", style = HushTheme.typography.body, color = colors.text)
            if (release.notes.isNotBlank()) {
                Text(release.notes.trim(), style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
            }
            if (release.apkSizeBytes > 0) {
                Text("${release.apkSizeBytes / 1_000_000} MB", style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                HushTextButton(actionLabel, onClick = onAction)
                HushTextButton("Überspringen", onClick = onSkip)
            }
        }
    }
}
