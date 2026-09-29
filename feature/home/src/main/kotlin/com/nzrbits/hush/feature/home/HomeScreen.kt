package com.nzrbits.hush.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.DateFormatChoice
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.model.ThemeMode
import com.nzrbits.hush.core.common.model.TimeFormatChoice
import com.nzrbits.hush.core.designsystem.components.HushAppItem
import com.nzrbits.hush.core.designsystem.components.HushDivider
import com.nzrbits.hush.core.designsystem.components.LedMatrixClock
import com.nzrbits.hush.core.designsystem.components.PixelGearButton
import com.nzrbits.hush.core.designsystem.scene.PixelSceneBackground
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.feature.apps.AppActionsNavigation
import com.nzrbits.hush.feature.apps.AppActionsSheet
import com.nzrbits.hush.feature.cozy.MascotBubble
import com.nzrbits.hush.feature.cozy.MascotState
import com.nzrbits.hush.feature.cozy.Sayings
import com.nzrbits.hush.feature.updates.HomeUpdateLine
import com.nzrbits.hush.feature.wellbeing.ui.BlockedNotice
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class HomeNavigation(
    val openDrawer: () -> Unit,
    val openSettings: () -> Unit,
    val openWellbeing: () -> Unit,
    val openNotificationLog: () -> Unit,
    val openPermissions: () -> Unit,
    val appActions: AppActionsNavigation,
)

/**
 * The home screen. Minimal Mode: clock, date, line, favourites. Cozy Mode adds Mr. Nook with
 * a saying, a warm palette and the optional pixel scene. Gestures live on the root box. The
 * favourites column scrolls only when it overflows, so short lists never steal the swipe.
 */
@Composable
fun HomeScreen(navigation: HomeNavigation, viewModel: HomeViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val blockStatus by viewModel.blockStatus.collectAsStateWithLifecycle()
    val blockedEvent by viewModel.blockedEvent.collectAsStateWithLifecycle()
    val charging by viewModel.charging.collectAsStateWithLifecycle()
    val wallpaper by viewModel.wallpaper.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    val cozy = settings.appearance.themeMode == ThemeMode.COZY

    // Back on the home screen does nothing. On Android 8 to 11 the default would finish the launcher.
    BackHandler(enabled = true) {}

    fun handle(result: GestureResult) {
        when (result) {
            GestureResult.OpenDrawer -> navigation.openDrawer()
            GestureResult.OpenSettings -> navigation.openSettings()
            GestureResult.OpenWellbeing -> navigation.openWellbeing()
            GestureResult.OpenNotificationLog -> navigation.openNotificationLog()
            GestureResult.NeedsAccessibility -> hint = "Dafür muss die Bedienungshilfe von ${HushConfig.APP_NAME} an sein. Tippen zum Einrichten."
            GestureResult.Done -> Unit
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(settings.gestures) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (abs(total) > swipeThresholdPx) {
                            handle(viewModel.perform(if (total < 0) settings.gestures.swipeUp else settings.gestures.swipeDown))
                        }
                    },
                    onVerticalDrag = { _, dragAmount -> total += dragAmount },
                )
            }
            .pointerInput(settings.gestures) {
                detectTapGestures(
                    onDoubleTap = { handle(viewModel.perform(settings.gestures.doubleTap)) },
                    onLongPress = { navigation.openSettings() },
                )
            },
    ) {
        wallpaper?.let { bitmap ->
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // Scrim so the clock stays readable on any photo.
            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.55f)))
        }
        PixelSceneBackground(
            scene = settings.appearance.scene,
            density = settings.appearance.sceneDensity,
            colors = colors,
            reduceMotion = settings.appearance.reduceMotion,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
        ) {
            // Clock sits right under the status bar; AM/PM is a small suffix, not part of the pixel digits.
            val (digits, suffix) = formatTime(now, settings.home.timeFormat, LocalContext.current)
            // LED matrix panel with the day in the calendar icon.
            LedMatrixClock(digits = digits, dayOfMonth = now.dayOfMonth)
            // Date, AM/PM and charging share one line at one size.
            val chargingText = when {
                !settings.home.showChargingAnimation || !charging.charging -> null
                charging.full || charging.percent >= 100 -> "Voll"
                else -> "Lädt ${charging.percent} %"
            }
            Text(
                text = listOfNotNull(formatDate(now, settings.home.dateFormat), suffix, chargingText).joinToString(" · "),
                style = HushTheme.typography.date,
                color = colors.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            // Small pixel gear under the date, right side.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PixelGearButton(color = colors.muted, onClick = navigation.openSettings)
            }
            if (!cozy) HushDivider()

            HomeUpdateLine(showMascot = cozy && settings.appearance.showMascot)

            blockedEvent?.let { event ->
                Spacer(Modifier.height(16.dp))
                BlockedNotice(
                    event = event,
                    appLabel = viewModel.labelFor(event.packageName),
                    showMascot = cozy && settings.appearance.showMascot,
                    onDismiss = viewModel::dismissBlocked,
                    onOpenWellbeing = { viewModel.dismissBlocked(); navigation.openWellbeing() },
                )
            }

            Spacer(Modifier.height(16.dp))
            val blockedPackages = remember(blockStatus) {
                blockStatus.manual.map { it.packageName }.toSet() + blockStatus.scheduled.flatMap { it.first.packageNames }.toSet()
            }
            // Takes all remaining height, so the quick actions stay at the bottom. Scrolling is enabled
            // only when the list overflows; otherwise vertical drags reach the root swipe gestures.
            val favoritesScroll = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(favoritesScroll, enabled = favoritesScroll.maxValue > 0),
            ) {
                val list = favorites
                if (list != null && list.isEmpty()) {
                    Text(
                        "Keine Favoriten. Nach oben wischen, App lang drücken, „Zu Favoriten hinzufügen“.",
                        style = HushTheme.typography.body,
                        color = colors.muted,
                    )
                }
                list?.forEach { app ->
                    HushAppItem(
                        label = app.displayLabel,
                        dimmed = app.packageName in blockedPackages,
                        secondary = if (app.packageName in blockedPackages) "gesperrt" else null,
                        onClick = { viewModel.launch(app) },
                        onLongClick = { selected = app },
                    )
                }
            }

            // Mr. Nook lives at the bottom, above the left shortcut.
            if (cozy && settings.appearance.showMascot) {
                val focusActive = blockStatus.scheduled.isNotEmpty()
                val screenTime by viewModel.screenTimeToday.collectAsStateWithLifecycle()
                MascotBubble(
                    text = if (focusActive) Sayings.focus(now) else Sayings.home(now, screenTime),
                    state = if (focusActive) MascotState.SLEEP else MascotState.IDLE,
                    onTap = navigation.openWellbeing,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            // Shortcuts in the corners: one left, one right, a third in the middle. Same size as the app list.
            val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                shortcuts.forEach { s -> QuickAction(s.label) { viewModel.openShortcut(s) } }
            }
            hint?.let {
                Text(
                    it,
                    style = HushTheme.typography.caption,
                    color = colors.muted,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { hint = null; navigation.openPermissions() },
                )
                LaunchedEffect(it) { kotlinx.coroutines.delay(6_000); hint = null }
            }
        }
    }

    selected?.let { app ->
        AppActionsSheet(app = app, navigation = navigation.appActions, onDismiss = { selected = null })
    }
}

/** Shortcut in a bottom corner: same size and colour as the app list, 48 dp tall. */
@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            style = HushTheme.typography.listItem,
            color = HushTheme.colors.muted,
            maxLines = 1,
        )
    }
}

/** Digits and an optional AM/PM suffix, kept apart so the pixel clock stays compact. */
fun formatTime(now: LocalDateTime, choice: TimeFormatChoice, context: android.content.Context): Pair<String, String?> {
    val use24 = when (choice) {
        TimeFormatChoice.H24 -> true
        TimeFormatChoice.H12 -> false
        TimeFormatChoice.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
    }
    return if (use24) {
        now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.GERMAN)) to null
    } else {
        now.format(DateTimeFormatter.ofPattern("h:mm", Locale.GERMAN)) to now.format(DateTimeFormatter.ofPattern("a", Locale.ENGLISH))
    }
}

fun formatDate(now: LocalDateTime, choice: DateFormatChoice): String = when (choice) {
    DateFormatChoice.LONG -> now.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))
    DateFormatChoice.SHORT -> now.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN))
    DateFormatChoice.NUMERIC -> now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
}
