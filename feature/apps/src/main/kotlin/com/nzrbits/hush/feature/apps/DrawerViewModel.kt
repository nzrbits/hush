package com.nzrbits.hush.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.model.AppFolder
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.text.Search
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
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
    val loaded: Boolean = false,
    val query: String = "",
    val sections: List<DrawerSection> = emptyList(),
    val letters: List<Char> = emptyList(),
    val folders: List<AppFolder> = emptyList(),
    /** Null until settings are known, so the keyboard is not opened against the user's choice. */
    val autoKeyboard: Boolean? = null,
    val totalApps: Int = 0,
    val blockedPackages: Set<String> = emptySet(),
)

/** Precomputed search index entry: normalised label is computed once per app list change. */
private data class Indexed(val app: LauncherApp, val normalized: String)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val apps: AppsRepository,
    settings: SettingsRepository,
    private val blocking: BlockingRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    private val indexed = apps.visibleApps.map { list -> list.map { Indexed(it, Search.normalize(it.displayLabel)) } }

    private val blocked = blocking.status.map { status ->
        status.manual.map { it.packageName }.toSet() + status.scheduled.flatMap { it.first.packageNames }.toSet()
    }

    val state: StateFlow<DrawerUiState> = combine(indexed, apps.folders, query, settings.settings, blocked) { index, folders, q, s, blockedSet ->
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
                loaded = true,
                query = q,
                sections = sections,
                letters = loose.map { Search.indexLetter(it.app.displayLabel) }.distinct(),
                folders = folders,
                autoKeyboard = s.home.autoKeyboardInDrawer,
                totalApps = index.size,
                blockedPackages = blockedSet,
            )
        } else {
            val ranked = index.mapNotNull { entry ->
                Search.rank(entry.normalized, normalizedQuery)?.let { rank -> rank to entry.app }
            }.sortedWith(compareBy({ it.first }, { it.second.displayLabel }))
            DrawerUiState(
                loaded = true,
                query = q,
                sections = listOf(DrawerSection(null, null, ranked.map { it.second })),
                letters = emptyList(),
                folders = folders,
                autoKeyboard = s.home.autoKeyboardInDrawer,
                totalApps = index.size,
                blockedPackages = blockedSet,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    fun setQuery(value: String) { query.value = value }
    fun clearQuery() { query.value = "" }

    /** Launch the single result on Enter. Returns false when nothing matched. */
    fun launchFirstResult(): Boolean {
        val first = state.value.sections.firstOrNull()?.apps?.firstOrNull() ?: return false
        launch(first)
        return true
    }

    /** Same rule as the home screen: a blocked app is refused and reported, never opened. */
    fun launch(app: LauncherApp) = viewModelScope.launch {
        val reason = blocking.reasonFor(app.packageName)
        if (reason != null) blocking.reportBlocked(app.packageName, reason) else apps.launch(app)
    }
}

/** Shared write operations for the context menu, used from home and drawer. */
@HiltViewModel
class AppActionsViewModel @Inject constructor(
    private val apps: AppsRepository,
    private val blocking: BlockingRepository,
) : ViewModel() {
    val folders: StateFlow<List<AppFolder>> = apps.folders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun isProtected(packageName: String) = blocking.isProtected(packageName)

    fun launch(app: LauncherApp) = viewModelScope.launch {
        val reason = blocking.reasonFor(app.packageName)
        if (reason != null) blocking.reportBlocked(app.packageName, reason) else apps.launch(app)
    }
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
