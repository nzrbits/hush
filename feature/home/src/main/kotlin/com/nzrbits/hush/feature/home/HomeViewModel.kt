package com.nzrbits.hush.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.GestureAction
import com.nzrbits.hush.core.common.model.HushSettings
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.actions.AccessibilityBridge
import com.nzrbits.hush.core.system.actions.SystemActions
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.calendar.CalendarSource
import com.nzrbits.hush.core.system.calendar.NextEvent
import com.nzrbits.hush.feature.wellbeing.data.BlockStatus
import com.nzrbits.hush.feature.wellbeing.data.BlockedEvent
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.max

data class ChargingState(val charging: Boolean, val full: Boolean, val percent: Int)

data class ResolvedShortcut(val token: String, val label: String, val app: LauncherApp?)

sealed interface CalendarState {
    data object NoPermission : CalendarState
    data object Empty : CalendarState
    data class Event(val event: NextEvent) : CalendarState
}

/** What a gesture should do, resolved by the screen because some actions navigate. */
sealed interface GestureResult {
    data object OpenDrawer : GestureResult
    data object OpenSettings : GestureResult
    data object OpenWellbeing : GestureResult
    data object OpenNotificationLog : GestureResult
    data object NeedsAccessibility : GestureResult
    data object Done : GestureResult
}

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val dispatchers: AppDispatchers,
    private val usage: com.nzrbits.hush.core.system.usage.UsageStatsSource,
    private val calendarSource: CalendarSource,
) : ViewModel() {
    val settings: StateFlow<HushSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HushSettings())

    /** Null until the first list arrived, so the empty state never flashes on start. */
    val favorites: StateFlow<List<LauncherApp>?> = apps.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val blockStatus: StateFlow<BlockStatus> = blocking.status.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockStatus(emptyList(), emptyList()))

    /** Ticks at every minute boundary so the clock text is always exact. */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(clock.nowLocal().toLocalDateTime())
            val ms = clock.now().toEpochMilli()
            delay(60_000L - (ms % 60_000L) + 20)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), clock.nowLocal().toLocalDateTime())

    private val calendarRefresh = MutableStateFlow(0)

    /** Next Google-calendar event for Mr. Nook, refreshed every five minutes and on demand. */
    val calendar: StateFlow<CalendarState> = combine(now.map { it.minute / 5 }.distinctUntilChanged(), calendarRefresh) { _, _ -> Unit }
        .mapLatest {
            if (!calendarSource.hasPermission()) CalendarState.NoPermission
            else calendarSource.nextEvent()?.let { CalendarState.Event(it) } ?: CalendarState.Empty
        }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), if (calendarSource.hasPermission()) CalendarState.Empty else CalendarState.NoPermission)

    fun refreshCalendar() { calendarRefresh.value++ }
    fun openEvent(event: NextEvent) = calendarSource.openEvent(event)

    /** Today's screen time for the mascot line, refreshed every five minutes. Null without usage access. */
    val screenTimeToday: StateFlow<String?> = now
        .map { it.minute / 5 }
        .distinctUntilChanged()
        .mapLatest {
            if (!usage.hasUsageAccess()) null else runCatching { usage.today().totalMillis }.getOrNull()?.let { ms ->
                if (ms < 60_000) null else com.nzrbits.hush.core.common.time.Durations.formatMillisShort(ms)
            }
        }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A blocked notice shows for 20 seconds, and never past the end of the block. */
    val blockedEvent: StateFlow<BlockedEvent?> = blocking.lastBlocked
        .mapLatest { event ->
            if (event == null) return@mapLatest null
            val shownFor = clock.now().toEpochMilli() - event.atMillis
            if (shownFor > NOTICE_MILLIS) return@mapLatest null
            delay(max(0L, NOTICE_MILLIS - shownFor))
            blockingRepository.clearBlockedEvent()
            null
        }
        .let { expiry -> combine(blocking.lastBlocked, expiry) { event, _ -> event } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Live battery state from ACTION_BATTERY_CHANGED, so plugging in shows at once. */
    val charging: StateFlow<ChargingState> = callbackFlow {
        fun parse(intent: Intent?): ChargingState {
            if (intent == null) return ChargingState(false, false, 0)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            return ChargingState(
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                full = status == BatteryManager.BATTERY_STATUS_FULL,
                percent = level * 100 / scale,
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) { trySend(parse(intent)) }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        trySend(parse(sticky))
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChargingState(false, false, 0))

    /** Wallpaper decoded once per URI on IO, sampled down to roughly screen size. */
    val wallpaper: StateFlow<ImageBitmap?> = settings.settings
        .map { it.appearance.wallpaperUri }
        .distinctUntilChanged()
        .mapLatest { uri -> uri?.let { decodeWallpaper(it) } }
        .flowOn(dispatchers.io)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun decodeWallpaper(uriString: String): ImageBitmap? = withContext(dispatchers.io) {
        runCatching {
            val uri = Uri.parse(uriString)
            val metrics = context.resources.displayMetrics
            val targetW = metrics.widthPixels.coerceAtLeast(1)
            val targetH = metrics.heightPixels.coerceAtLeast(1)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
        }.getOrNull()
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

    /** Resolved bottom-left shortcuts: built-in tokens get fixed labels, packages their app label. */
    val shortcuts: StateFlow<List<ResolvedShortcut>> = combine(settings.settings, apps.allApps) { s, all ->
        s.home.shortcuts.take(com.nzrbits.hush.core.common.model.Shortcuts.SLOTS).mapNotNull { token ->
            val builtIn = com.nzrbits.hush.core.common.model.Shortcuts.builtInLabel(token)
            when {
                builtIn != null -> ResolvedShortcut(token, builtIn, null)
                else -> all.firstOrNull { it.packageName == token }?.let { ResolvedShortcut(token, it.displayLabel, it) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openShortcut(shortcut: ResolvedShortcut) {
        when (shortcut.token) {
            com.nzrbits.hush.core.common.model.Shortcuts.PHONE -> systemActions.openDialer()
            com.nzrbits.hush.core.common.model.Shortcuts.CAMERA -> systemActions.openCamera()
            com.nzrbits.hush.core.common.model.Shortcuts.ALARM -> systemActions.openAlarms()
            else -> shortcut.app?.let { launch(it) }
        }
    }

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

    private companion object { const val NOTICE_MILLIS = 20_000L }
}
