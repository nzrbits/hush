package com.nzrbits.hush.core.system.apps

import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.AppFolder
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.database.AppCustomizationDao
import com.nzrbits.hush.core.database.AppCustomizationEntity
import com.nzrbits.hush.core.database.FolderDao
import com.nzrbits.hush.core.database.FolderEntity
import com.nzrbits.hush.core.database.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Joins system app list and Hush customisation. This is the single source used by the home
 * screen, drawer and settings. All writes go through here so the derived flows stay coherent.
 */
@Singleton
class AppsRepository @Inject constructor(
    private val source: InstalledAppsSource,
    private val customizationDao: AppCustomizationDao,
    private val folderDao: FolderDao,
    private val dispatchers: AppDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    /** Every launchable app (including hidden ones), sorted by display label. Shared so the system is queried once. */
    val allApps: Flow<List<LauncherApp>> = combine(source.apps, customizationDao.observeAll()) { installed, custom ->
        val byId = custom.associateBy { it.appId }
        installed.map { app ->
            val c = byId[app.key.id]
            LauncherApp(
                key = app.key,
                originalLabel = app.label,
                customLabel = c?.customLabel,
                hidden = c?.hidden ?: false,
                favoriteOrder = c?.favoriteOrder,
                folderId = c?.folderId,
                isWorkProfile = app.isWorkProfile,
                isSystemApp = app.isSystemApp,
                activityClassName = app.activityClassName,
            )
        }.sortedWith { a, b -> collator.compare(a.displayLabel, b.displayLabel) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val visibleApps: Flow<List<LauncherApp>> = allApps.map { list -> list.filter { !it.hidden } }

    /** Favourites are alphabetical like the drawer; allApps is already sorted with the collator. */
    val favorites: Flow<List<LauncherApp>> = allApps.map { list ->
        list.filter { it.isFavorite && !it.hidden }
    }

    val folders: Flow<List<AppFolder>> = folderDao.observeAll().map { it.map(FolderEntity::toModel) }

    suspend fun find(key: AppKey): LauncherApp? = allApps.first().firstOrNull { it.key == key }

    suspend fun findByPackage(packageName: String): LauncherApp? =
        allApps.first().firstOrNull { it.packageName == packageName }

    suspend fun launch(app: LauncherApp): Boolean = source.launch(app.key, app.activityClassName)

    fun openAppInfo(app: LauncherApp) = source.openAppInfo(app.key, app.activityClassName)

    fun requestUninstall(app: LauncherApp) = source.requestUninstall(app.key)

    fun labelFor(packageName: String): String = source.labelFor(packageName)

    private suspend fun existing(key: AppKey): AppCustomizationEntity =
        customizationDao.get(key.id) ?: AppCustomizationEntity(
            appId = key.id, packageName = key.packageName, userSerial = key.userSerial,
            customLabel = null, hidden = false, favoriteOrder = null, folderId = null,
        )

    suspend fun setFavorite(key: AppKey, favorite: Boolean) = withContext(dispatchers.io) {
        val current = existing(key)
        val order = if (favorite) (current.favoriteOrder ?: (customizationDao.maxFavoriteOrder() + 1)) else null
        customizationDao.upsert(current.copy(favoriteOrder = order))
    }

    suspend fun reorderFavorites(orderedKeys: List<AppKey>) = withContext(dispatchers.io) {
        customizationDao.reorderFavorites(orderedKeys.map { it.id })
    }

    suspend fun rename(key: AppKey, newLabel: String?) = withContext(dispatchers.io) {
        customizationDao.upsert(existing(key).copy(customLabel = newLabel?.trim()?.takeIf { it.isNotEmpty() }))
    }

    suspend fun setHidden(key: AppKey, hidden: Boolean) = withContext(dispatchers.io) {
        customizationDao.upsert(existing(key).copy(hidden = hidden))
    }

    suspend fun setFolder(key: AppKey, folderId: Long?) = withContext(dispatchers.io) {
        customizationDao.upsert(existing(key).copy(folderId = folderId))
    }

    suspend fun createFolder(name: String): Long = withContext(dispatchers.io) {
        val count = folderDao.observeAll().first().size
        folderDao.insert(FolderEntity(name = name.trim(), sortOrder = count))
    }

    suspend fun renameFolder(id: Long, name: String) = withContext(dispatchers.io) { folderDao.rename(id, name.trim()) }

    suspend fun deleteFolder(id: Long) = withContext(dispatchers.io) {
        customizationDao.clearFolder(id)
        folderDao.delete(id)
    }
}
