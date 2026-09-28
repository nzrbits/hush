package com.nzrbits.hush.feature.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.designsystem.components.HushConfirmDialog
import com.nzrbits.hush.core.designsystem.components.HushDialog
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushTextInputDialog
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/** Navigation hooks the sheet needs from the host. Wellbeing screens live in another module. */
data class AppActionsNavigation(
    val onBlockApp: (packageName: String) -> Unit,
    val onSetLimit: (packageName: String) -> Unit,
)

/**
 * Long-press context menu for an app. Owns its dialogs so home and drawer only pass the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: LauncherApp,
    navigation: AppActionsNavigation,
    onDismiss: () -> Unit,
    viewModel: AppActionsViewModel = hiltViewModel(),
) {
    val colors = HushTheme.colors
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.text,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(app.displayLabel, style = HushTheme.typography.title, color = colors.text)
            Text(
                if (app.customLabel != null) "${app.originalLabel} · ${app.packageName}" else app.packageName,
                style = HushTheme.typography.caption,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            HushRow("Öffnen", onClick = { viewModel.launch(app); onDismiss() })
            if (app.isFavorite) {
                HushRow("Aus Favoriten entfernen", onClick = { viewModel.setFavorite(app.key, false); onDismiss() })
            } else {
                HushRow("Zu Favoriten hinzufügen", onClick = { viewModel.setFavorite(app.key, true); onDismiss() })
            }
            HushRow("Umbenennen", onClick = { dialog = Dialog.Rename })
            HushRow(if (app.hidden) "Einblenden" else "Ausblenden", onClick = { viewModel.setHidden(app.key, !app.hidden); onDismiss() })
            HushRow("In Ordner verschieben", subtitle = folders.firstOrNull { it.id == app.folderId }?.name, onClick = { dialog = Dialog.Folder })
            HushRow("Zeiterinnerung", subtitle = "Tägliches Limit als Erinnerung", onClick = { navigation.onSetLimit(app.packageName); onDismiss() })
            HushRow("App blockieren", subtitle = "1 Stunde bis 30 Tage", onClick = { navigation.onBlockApp(app.packageName); onDismiss() })
            HushRow("App-Info", onClick = { viewModel.openAppInfo(app); onDismiss() })
            HushRow("Deinstallieren", onClick = { dialog = Dialog.Uninstall })
        }
    }

    when (dialog) {
        Dialog.Rename -> HushTextInputDialog(
            title = "Umbenennen",
            initial = app.customLabel ?: app.originalLabel,
            placeholder = app.originalLabel,
            onDismiss = { dialog = null },
            onConfirm = { value ->
                viewModel.rename(app.key, if (value.trim() == app.originalLabel) null else value)
                dialog = null
                onDismiss()
            },
        )
        Dialog.Folder -> FolderPickerDialog(
            current = app.folderId,
            folders = folders.map { it.id to it.name },
            onDismiss = { dialog = null },
            onPick = { id -> viewModel.setFolder(app.key, id); dialog = null; onDismiss() },
            onCreate = { name -> viewModel.createFolderAndMove(app.key, name); dialog = null; onDismiss() },
        )
        Dialog.Uninstall -> HushConfirmDialog(
            title = "Deinstallieren?",
            text = "Android zeigt gleich den Systemdialog zum Entfernen von ${app.displayLabel}.",
            confirmLabel = "Weiter",
            danger = true,
            onDismiss = { dialog = null },
            onConfirm = { viewModel.uninstall(app); dialog = null; onDismiss() },
        )
        null -> Unit
    }
}

private enum class Dialog { Rename, Folder, Uninstall }

@Composable
private fun FolderPickerDialog(
    current: Long?,
    folders: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit,
    onCreate: (String) -> Unit,
) {
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        HushTextInputDialog(
            title = "Neuer Ordner",
            initial = "",
            placeholder = "Name",
            confirmLabel = "Anlegen",
            onDismiss = { creating = false },
            onConfirm = { name -> if (name.isNotBlank()) onCreate(name) else creating = false },
        )
        return
    }
    HushDialog(title = "Ordner", onDismiss = onDismiss) {
        HushRow("Kein Ordner", trailing = if (current == null) "•" else null, onClick = { onPick(null) })
        folders.forEach { (id, name) ->
            HushRow(name, trailing = if (current == id) "•" else null, onClick = { onPick(id) })
        }
        HushRow("Neuer Ordner …", onClick = { creating = true })
    }
}
