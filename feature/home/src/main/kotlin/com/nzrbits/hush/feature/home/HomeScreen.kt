package com.nzrbits.hush.feature.home

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.model.DateFormatChoice
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.model.ThemeMode
import com.nzrbits.hush.core.common.model.TimeFormatChoice
import com.nzrbits.hush.core.designsystem.components.HushAppItem
import com.nzrbits.hush.core.designsystem.components.HushDivider
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.scene.PixelSceneBackground
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.feature.apps.AppActionsNavigation
import com.nzrbits.hush.feature.apps.AppActionsSheet
import com.nzrbits.hush.feature.cozy.MascotBubble
import com.nzrbits.hush.feature.cozy.MascotState
import com.nzrbits.hush.feature.cozy.Sayings
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
 * a saying, a warm palette and the optional pixel scene. Gestures live on the root box; the
 * favourites column is not scrollable, so drags never fight a list.
 */
@Composable
fun HomeScreen(navigation: HomeNavigation, viewModel: HomeViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val blockStatus by viewModel.blockStatus.collectAsStateWithLifecycle()
    val blockedEvent by viewModel.blockedEvent.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<LauncherApp?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    val cozy = settings.appearance.themeMode == ThemeMode.COZY

    fun handle(result: GestureResult) {
        when (result) {
            GestureResult.OpenDrawer -> navigation.openDrawer()
            GestureResult.OpenSettings -> navigation.openSettings()
            GestureResult.OpenWellbeing -> navigation.openWellbeing()
            GestureResult.OpenNotificationLog -> navigation.openNotificationLog()
            GestureResult.NeedsAccessibility -> hint = "Dafür muss die Bedienungshilfe von Hush an sein."
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
        settings.appearance.wallpaperUri?.let { WallpaperImage(it) }
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
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = formatTime(now, settings.home.timeFormat, LocalContext.current),
                style = HushTheme.typography.clock,
                color = colors.text,
            )
            Text(
                text = formatDate(now, settings.home.dateFormat),
                style = HushTheme.typography.date,
                color = colors.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
            val charging = remember(now) { viewModel.chargingState() }
            if (settings.home.showChargingAnimation && charging.charging) {
                Text("Lädt · ${charging.percent} %", style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
            }

            if (cozy && settings.appearance.showMascot) {
                Spacer(Modifier.height(20.dp))
                val focusActive = blockStatus.scheduled.isNotEmpty()
                MascotBubble(
                    text = if (focusActive) Sayings.focus(now) else Sayings.home(now),
                    state = if (focusActive) MascotState.SLEEP else MascotState.IDLE,
                    onTap = navigation.openWellbeing,
                )
            } else {
                Spacer(Modifier.height(20.dp))
                HushDivider()
            }

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
            if (favorites.isEmpty()) {
                Text(
                    "Keine Favoriten. Nach oben wischen, App lang drücken, „Zu Favoriten hinzufügen“.",
                    style = HushTheme.typography.body,
                    color = colors.muted,
                )
            }
            val blockedPackages = remember(blockStatus) {
                blockStatus.manual.map { it.packageName }.toSet() + blockStatus.scheduled.flatMap { it.first.packageNames }.toSet()
            }
            favorites.forEach { app ->
                HushAppItem(
                    label = app.displayLabel,
                    dimmed = app.packageName in blockedPackages,
                    secondary = if (app.packageName in blockedPackages) "gesperrt" else null,
                    onClick = { viewModel.launch(app) },
                    onLongClick = { selected = app },
                )
            }

            Spacer(Modifier.weight(1f))

            if (cozy) {
                HushTextButton("Fokus & Bildschirmzeit", onClick = navigation.openWellbeing)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row {
                    if (settings.home.showPhone) QuickAction("Telefon") { viewModel.openDialer() }
                    if (settings.home.showAlarm) QuickAction("Wecker") { viewModel.openAlarms() }
                }
                Row {
                    if (settings.home.showCamera) QuickAction("Kamera") { viewModel.openCamera() }
                }
            }
            hint?.let {
                Text(it, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
                LaunchedEffect(it) { kotlinx.coroutines.delay(4_000); hint = null }
            }
        }
    }

    selected?.let { app ->
        AppActionsSheet(app = app, navigation = navigation.appActions, onDismiss = { selected = null })
    }
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = HushTheme.typography.body.copy(fontWeight = FontWeight.Medium),
        color = HushTheme.colors.muted,
        modifier = Modifier
            .padding(end = 20.dp, top = 12.dp, bottom = 4.dp)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
    )
}

@Composable
private fun WallpaperImage(uri: String) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
    bitmap?.let { Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
}

fun formatTime(now: LocalDateTime, choice: TimeFormatChoice, context: android.content.Context): String {
    val use24 = when (choice) {
        TimeFormatChoice.H24 -> true
        TimeFormatChoice.H12 -> false
        TimeFormatChoice.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
    }
    return now.format(DateTimeFormatter.ofPattern(if (use24) "HH:mm" else "h:mm", Locale.GERMAN))
}

fun formatDate(now: LocalDateTime, choice: DateFormatChoice): String = when (choice) {
    DateFormatChoice.LONG -> now.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))
    DateFormatChoice.SHORT -> now.format(DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN))
    DateFormatChoice.NUMERIC -> now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN))
}
