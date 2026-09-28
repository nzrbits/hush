package com.nzrbits.hush.feature.apps

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.designsystem.components.HushEmptyState
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushScreen
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.core.system.apps.AppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageAppsViewModel @Inject constructor(private val apps: AppsRepository) : ViewModel() {
    val favorites: StateFlow<List<LauncherApp>> = apps.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val all: StateFlow<List<LauncherApp>> = apps.allApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun move(key: AppKey, delta: Int) = viewModelScope.launch {
        val current = favorites.value.map { it.key }.toMutableList()
        val index = current.indexOf(key)
        val target = index + delta
        if (index < 0 || target < 0 || target >= current.size) return@launch
        current.removeAt(index)
        current.add(target, key)
        apps.reorderFavorites(current)
    }

    fun setFavorite(key: AppKey, favorite: Boolean) = viewModelScope.launch { apps.setFavorite(key, favorite) }
    fun setHidden(key: AppKey, hidden: Boolean) = viewModelScope.launch { apps.setHidden(key, hidden) }
}

/** Settings > Homescreen > Favoriten: reorder and remove, add from the full list. */
@Composable
fun FavoritesScreen(onBack: () -> Unit, viewModel: ManageAppsViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    val colors = HushTheme.colors

    HushScreen(title = "Favoriten", onBack = onBack) {
        if (favorites.isEmpty()) HushEmptyState("Noch keine Favoriten. Lang auf eine App drücken oder unten hinzufügen.")
        favorites.forEachIndexed { index, app ->
            HushRow(
                title = app.displayLabel,
                subtitle = if (app.isWorkProfile) "Arbeitsprofil" else null,
                trailingContent = {
                    Row {
                        HushTextButton("↑", onClick = { viewModel.move(app.key, -1) })
                        HushTextButton("↓", onClick = { viewModel.move(app.key, +1) })
                        HushTextButton("Entfernen", onClick = { viewModel.setFavorite(app.key, false) }, danger = true)
                    }
                },
            )
        }
        HushSectionHeader("Hinzufügen")
        HushRow(if (adding) "Liste ausblenden" else "App aus Liste wählen", onClick = { adding = !adding })
        if (adding) {
            all.filter { !it.isFavorite && !it.hidden }.forEach { app ->
                HushRow(app.displayLabel, onClick = { viewModel.setFavorite(app.key, true) })
            }
        }
        Text(
            "Favoriten stehen als Textliste auf dem Startbildschirm.",
            style = HushTheme.typography.caption,
            color = colors.muted,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Settings > Homescreen > Verborgene Apps. */
@Composable
fun HiddenAppsScreen(onBack: () -> Unit, viewModel: ManageAppsViewModel = hiltViewModel()) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val hidden = all.filter { it.hidden }
    var adding by remember { mutableStateOf(false) }
    HushScreen(title = "Verborgene Apps", onBack = onBack) {
        if (hidden.isEmpty()) HushEmptyState("Keine App ist ausgeblendet.")
        hidden.forEach { app ->
            HushRow(app.displayLabel, trailingContent = { HushTextButton("Einblenden", onClick = { viewModel.setHidden(app.key, false) }) })
        }
        HushSectionHeader("Ausblenden")
        HushRow(if (adding) "Liste ausblenden" else "App aus Liste wählen", onClick = { adding = !adding })
        if (adding) {
            all.filter { !it.hidden }.forEach { app ->
                HushRow(app.displayLabel, onClick = { viewModel.setHidden(app.key, true) })
            }
        }
    }
}
