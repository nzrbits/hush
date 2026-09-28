package com.nzrbits.hush.feature.wellbeing.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.AppUsage
import com.nzrbits.hush.core.common.model.BlockSchedule
import com.nzrbits.hush.core.common.model.DailyUsage
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.model.ShortVideoPlatform
import com.nzrbits.hush.core.common.model.UsageLimit
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.permissions.PermissionsChecker
import com.nzrbits.hush.core.system.usage.UsageStatsSource
import com.nzrbits.hush.feature.wellbeing.data.BlockStatus
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import com.nzrbits.hush.feature.wellbeing.data.UsageLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/** Shared state for the hub, block and screen time screens. */
data class UsageSnapshot(
    val hasUsageAccess: Boolean = false,
    val today: DailyUsage? = null,
    val week: List<DailyUsage> = emptyList(),
    val labels: Map<String, String> = emptyMap(),
)

@HiltViewModel
class WellbeingHubViewModel @Inject constructor(
    private val blocking: BlockingRepository,
    private val usageSource: UsageStatsSource,
    private val apps: AppsRepository,
    private val permissions: PermissionsChecker,
) : ViewModel() {
    val status: StateFlow<BlockStatus> = blocking.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockStatus(emptyList(), emptyList()))
    private val _usage = MutableStateFlow(UsageSnapshot())
    val usage: StateFlow<UsageSnapshot> = _usage
    val accessibilityOn: Boolean get() = permissions.isAccessibilityEnabled()

    fun refresh() = viewModelScope.launch {
        val has = usageSource.hasUsageAccess()
        val today = if (has) usageSource.today() else null
        val week = if (has) usageSource.lastDays(7) else emptyList()
        val packages = (today?.perApp.orEmpty() + week.flatMap { it.perApp }).map { it.packageName }.toSet()
        _usage.value = UsageSnapshot(has, today, week, packages.associateWith { apps.labelFor(it) })
    }

    fun unblock(id: Long) = viewModelScope.launch { blocking.unblock(id) }
    fun labelFor(packageName: String) = apps.labelFor(packageName)
}

@HiltViewModel
class BlockAppViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val blocking: BlockingRepository,
    private val usage: UsageStatsSource,
    private val apps: AppsRepository,
    private val permissions: PermissionsChecker,
) : ViewModel() {
    val packageName: String = savedState.get<String>("packageName").orEmpty()
    val label: String = apps.labelFor(packageName)
    val minutes = MutableStateFlow(HushConfig.MIN_BLOCK_MINUTES)
    private val _todayMillis = MutableStateFlow<Long?>(null)
    val todayMillis: StateFlow<Long?> = _todayMillis
    private val _weekMillis = MutableStateFlow<List<Long>>(emptyList())
    val weekMillis: StateFlow<List<Long>> = _weekMillis
    val activeBlock = blocking.activeBlocks.map { list -> list.firstOrNull { it.packageName == packageName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val accessibilityOn: Boolean get() = permissions.isAccessibilityEnabled()
    val hasUsageAccess: Boolean get() = usage.hasUsageAccess()
    val isProtected: Boolean get() = blocking.isProtected(packageName)

    init {
        viewModelScope.launch {
            if (usage.hasUsageAccess()) {
                _todayMillis.value = usage.todayForPackage(packageName)
                _weekMillis.value = usage.lastDays(7).map { day -> day.perApp.firstOrNull { it.packageName == packageName }?.foregroundMillis ?: 0L }
            }
        }
    }

    fun setMinutes(value: Long) { minutes.value = value }

    fun confirm(onDone: () -> Unit) = viewModelScope.launch {
        blocking.block(packageName, minutes.value)
        onDone()
    }

    fun unblock(id: Long) = viewModelScope.launch { blocking.unblock(id) }
}

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val blocking: BlockingRepository,
    private val apps: AppsRepository,
) : ViewModel() {
    val schedules: StateFlow<List<BlockSchedule>> = blocking.schedules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val status: StateFlow<BlockStatus> = blocking.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockStatus(emptyList(), emptyList()))
    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch { blocking.setScheduleEnabled(id, enabled) }
    fun delete(id: Long) = viewModelScope.launch { blocking.deleteSchedule(id) }
    fun labelFor(packageName: String) = apps.labelFor(packageName)
}

@HiltViewModel
class ScheduleEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val blocking: BlockingRepository,
    apps: AppsRepository,
) : ViewModel() {
    private val id: Long = savedState.get<String>("id")?.toLongOrNull() ?: 0L
    val apps: StateFlow<List<LauncherApp>> = apps.visibleApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val draft = MutableStateFlow(
        BlockSchedule(
            id = 0, name = "", enabled = true,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            startTime = LocalTime.of(9, 0), endTime = LocalTime.of(12, 0), packageNames = emptySet(),
        ),
    )
    val isNew: Boolean get() = id == 0L

    init {
        if (id != 0L) viewModelScope.launch { blocking.schedule(id)?.let { draft.value = it } }
    }

    fun isProtected(packageName: String) = blocking.isProtected(packageName)

    fun update(transform: (BlockSchedule) -> BlockSchedule) { draft.value = transform(draft.value) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = draft.value
        blocking.saveSchedule(d.copy(name = d.name.ifBlank { "Fokuszeit" }))
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        if (id != 0L) blocking.deleteSchedule(id)
        onDone()
    }
}

@HiltViewModel
class LimitsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val limitRepository: UsageLimitRepository,
    private val appsRepository: AppsRepository,
    private val usage: UsageStatsSource,
    private val settings: SettingsRepository,
) : ViewModel() {
    val preselected: String? = savedState.get<String>("packageName")
    val limits: StateFlow<List<UsageLimit>> = limitRepository.limits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val apps: StateFlow<List<LauncherApp>> = appsRepository.visibleApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val remindersEnabled = settings.settings.map { it.wellbeing.usageLimitRemindersEnabled }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val hasUsageAccess: Boolean get() = usage.hasUsageAccess()

    fun labelFor(packageName: String) = appsRepository.labelFor(packageName)
    fun set(packageName: String, minutes: Int) = viewModelScope.launch { limitRepository.set(UsageLimit(packageName, minutes)) }
    fun remove(packageName: String) = viewModelScope.launch { limitRepository.remove(packageName) }
    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { settings.updateWellbeing { it.copy(usageLimitRemindersEnabled = enabled) } }
}

@HiltViewModel
class ShortVideoViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val permissions: PermissionsChecker,
) : ViewModel() {
    val wellbeing = settings.settings.map { it.wellbeing }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.nzrbits.hush.core.common.model.WellbeingSettings())
    val accessibilityOn: Boolean get() = permissions.isAccessibilityEnabled()
    fun setEnabled(enabled: Boolean) = viewModelScope.launch { settings.updateWellbeing { it.copy(shortVideoBlockingEnabled = enabled) } }
    fun togglePlatform(platform: ShortVideoPlatform, on: Boolean) = viewModelScope.launch {
        settings.updateWellbeing { it.copy(shortVideoPlatforms = if (on) it.shortVideoPlatforms + platform else it.shortVideoPlatforms - platform) }
    }
}

/** Helper for screens: top apps of a day as label/duration pairs. */
fun DailyUsage.top(labels: Map<String, String>, n: Int = 8): List<Pair<String, AppUsage>> =
    perApp.take(n).map { (labels[it.packageName] ?: it.packageName) to it }
