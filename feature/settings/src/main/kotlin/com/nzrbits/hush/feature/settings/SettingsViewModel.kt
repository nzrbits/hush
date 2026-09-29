package com.nzrbits.hush.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.model.AppearanceSettings
import com.nzrbits.hush.core.common.model.GestureSettings
import com.nzrbits.hush.core.common.model.HomeSettings
import com.nzrbits.hush.core.common.model.HushSettings
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.model.WellbeingSettings
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.actions.SystemActions
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.permissions.HushPermission
import com.nzrbits.hush.core.system.permissions.PermissionState
import com.nzrbits.hush.core.system.permissions.PermissionsChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val permissions: PermissionsChecker,
    private val systemActions: SystemActions,
    appsRepository: AppsRepository,
) : ViewModel() {
    val settings: StateFlow<HushSettings> = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HushSettings())
    val apps: StateFlow<List<LauncherApp>> = appsRepository.visibleApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _permissions = MutableStateFlow(permissions.snapshot())
    val permissionStates: StateFlow<List<PermissionState>> = _permissions

    fun refreshPermissions() { _permissions.value = permissions.snapshot() }

    fun updateHome(t: (HomeSettings) -> HomeSettings) = viewModelScope.launch { repository.updateHome(t) }
    fun updateAppearance(t: (AppearanceSettings) -> AppearanceSettings) = viewModelScope.launch { repository.updateAppearance(t) }
    fun updateGestures(t: (GestureSettings) -> GestureSettings) = viewModelScope.launch { repository.updateGestures(t) }
    fun updateWellbeing(t: (WellbeingSettings) -> WellbeingSettings) = viewModelScope.launch { repository.updateWellbeing(t) }
    fun setOnboardingDone() = viewModelScope.launch { repository.setOnboardingDone(true) }

    /** For every permission except DEFAULT_LAUNCHER, which needs [defaultLauncherIntent] via an activity result launcher. */
    fun openPermission(permission: HushPermission) {
        runCatching { context.startActivity(permissions.settingsIntent(permission)) }
    }

    fun defaultLauncherIntent(): Intent = permissions.defaultLauncherIntent()
    fun isDefaultLauncher(): Boolean = permissions.isDefaultLauncher()

    /** "Launcher verlassen": open the system home chooser so the user can pick another launcher. */
    fun leaveLauncher() { systemActions.openSystemHomeSettings() }

    /** Called only with a picked URI; cancelling the picker keeps the current wallpaper. */
    fun persistWallpaper(uri: android.net.Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        updateAppearance { it.copy(wallpaperUri = uri.toString()) }
    }

    fun clearWallpaper() = updateAppearance { it.copy(wallpaperUri = null) }
}
