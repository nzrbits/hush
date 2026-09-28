package com.nzrbits.hush.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.model.AppFolder
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.text.Search
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.apps.AppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DrawerSection(val title: String?, val folderId: Long?, val apps: List<LauncherApp>)

data class DrawerUiState(
    val query: String = "",
    val sections: List<DrawerSection> = emptyList(),
    val letters: List<Char> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    val autoKeyboard: Boolean = true,
    val totalApps: Int = 0,
)

/** Precomputed search index entry: normalised label is computed once per app list change. */
private data class Indexed(val app: LauncherApp, val normalized: String)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val apps: AppsRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val indexed = apps.visibleApps.map { list -> list.map { Indexed(it, Search.normalize(it.displayLabel)) } }

    val state: StateFlow<DrawerUiState> = combine(indexed, apps.folders, query, settings.settings) { index, folders, q, s ->
        val normalizedQuery = Search.normalize(q)
        if (normalizedQuery.isEmpty()) {
            val folderMap = folders.associateBy { it.id }
            val inFolders = index.filter { it.app.folderId != null && folderMap.containsKey(it.app.folderId) }
            val loose = index.filter { it.app.folderId == null || !folderMap.containsKey(it.app.folderId) }
            val sections = buildList {
                folders.forEach { folder ->
                    val members = inFolders.filter { it.app.folderId == folder.id }.map { it.app }
                    if (members.isNotEmpty()) add(DrawerSection(folder.name, folder.id, members))
                }
                add(DrawerSection(if (folders.isEmpty()) null else "Alle Apps", null, loose.map { it.app }))
            }
            DrawerUiState(
                query = q,
                sections = sections,
                letters = loose.map { Search.indexLetter(it.app.displayLabel) }.distinct(),
                folders = folders,
                autoKeyboard = s.home.autoKeyboardInDrawer,
                totalApps = index.size,
            )
        } else {
            val ranked = index.mapNotNull { entry ->
                Search.rank(entry.normalized, normalizedQuery)?.let { rank -> rank to entry.app }
            }.sortedWith(compareBy({ it.first }, { it.second.displayLabel }))
            DrawerUiState(
                query = q,
                sections = listOf(DrawerSection(null, null, ranked.map { it.second })),
                letters = emptyList(),
                folders = folders,
                autoKeyboard = s.home.autoKeyboardInDrawer,
                totalApps = index.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    fun setQuery(value: String) { query.value = value }
    fun clearQuery() { query.value = "" }

    /** Launch the single result on Enter. */
    fun launchFirstResult(): Boolean {
        val first = state.value.sections.firstOrNull()?.apps?.firstOrNull() ?: return false
        launch(first)
        return true
    }

    fun launch(app: LauncherApp) { viewModelScope.launch { apps.launch(app) } }
}

/** Shared write operations for the context menu, used from home and drawer. */
@HiltViewModel
class AppActionsViewModel @Inject constructor(
    private val apps: AppsRepository,
) : ViewModel() {
    val folders: StateFlow<List<AppFolder>> = apps.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun launch(app: LauncherApp) = viewModelScope.launch { apps.launch(app) }
    fun setFavorite(key: AppKey, favorite: Boolean) = viewModelScope.launch { apps.setFavorite(key, favorite) }
    fun rename(key: AppKey, label: String?) = viewModelScope.launch { apps.rename(key, label) }
    fun setHidden(key: AppKey, hidden: Boolean) = viewModelScope.launch { apps.setHidden(key, hidden) }
    fun setFolder(key: AppKey, folderId: Long?) = viewModelScope.launch { apps.setFolder(key, folderId) }
    fun createFolderAndMove(key: AppKey, name: String) = viewModelScope.launch {
        val id = apps.createFolder(name)
        apps.setFolder(key, id)
    }
    fun openAppInfo(app: LauncherApp) = apps.openAppInfo(app)
    fun uninstall(app: LauncherApp) = apps.requestUninstall(app)
}
