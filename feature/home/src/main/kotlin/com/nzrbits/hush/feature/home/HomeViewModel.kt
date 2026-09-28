package com.nzrbits.hush.feature.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.model.GestureAction
import com.nzrbits.hush.core.common.model.HushSettings
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.actions.AccessibilityBridge
import com.nzrbits.hush.core.system.actions.SystemActions
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.feature.wellbeing.data.BlockStatus
import com.nzrbits.hush.feature.wellbeing.data.BlockedEvent
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChargingState(val charging: Boolean, val percent: Int)

/** What a gesture should do, resolved by the screen because some actions navigate. */
sealed interface GestureResult {
    data object OpenDrawer : GestureResult
    data object OpenSettings : GestureResult
    data object OpenWellbeing : GestureResult
    data object OpenNotificationLog : GestureResult
    data object NeedsAccessibility : GestureResult
    data object Done : GestureResult
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apps: AppsRepository,
    settings: SettingsRepository,
    blocking: BlockingRepository,
    private val clock: HushClock,
    private val bridge: AccessibilityBridge,
    private val systemActions: SystemActions,
    private val blockingRepository: BlockingRepository,
) : ViewModel() {
    val settings: StateFlow<HushSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HushSettings())
    val favorites: StateFlow<List<LauncherApp>> = apps.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val blockStatus: StateFlow<BlockStatus> = blocking.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockStatus(emptyList(), emptyList()))
    val blockedEvent: StateFlow<BlockedEvent?> = blocking.lastBlocked

    /** Ticks at every minute boundary so the clock text is always exact. */
    val now: StateFlow<java.time.LocalDateTime> = flow {
        while (true) {
            emit(clock.nowLocal().toLocalDateTime())
            val ms = clock.now().toEpochMilli()
            delay(60_000L - (ms % 60_000L) + 20)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), clock.nowLocal().toLocalDateTime())

    fun chargingState(): ChargingState {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return ChargingState(false, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        return ChargingState(charging, level * 100 / scale)
    }

    fun launch(app: LauncherApp) = viewModelScope.launch {
        // Blocked apps are refused here too, so the launcher never opens what the service would close.
        val reason = blockingRepository.reasonFor(app.packageName)
        if (reason != null) {
            blockingRepository.reportBlocked(app.packageName, reason)
        } else {
            apps.launch(app)
        }
    }

    fun dismissBlocked() = blockingRepository.clearBlockedEvent()

    fun labelFor(packageName: String) = apps.labelFor(packageName)

    fun openDialer() = systemActions.openDialer()
    fun openCamera() = systemActions.openCamera()
    fun openAlarms() = systemActions.openAlarms()

    fun perform(action: GestureAction): GestureResult = performLogged(action).also { android.util.Log.i("HushHome", "gesture $action -> $it") }

    private fun performLogged(action: GestureAction): GestureResult = when (action) {
        GestureAction.NONE -> GestureResult.Done
        GestureAction.OPEN_DRAWER -> GestureResult.OpenDrawer
        GestureAction.OPEN_SETTINGS -> GestureResult.OpenSettings
        GestureAction.OPEN_WELLBEING -> GestureResult.OpenWellbeing
        GestureAction.OPEN_NOTIFICATION_LOG -> GestureResult.OpenNotificationLog
        GestureAction.OPEN_NOTIFICATIONS -> if (bridge.openNotifications()) GestureResult.Done else GestureResult.NeedsAccessibility
        GestureAction.LOCK_SCREEN -> if (bridge.lockScreen()) GestureResult.Done else GestureResult.NeedsAccessibility
    }
}
