package com.nzrbits.hush.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nzrbits.hush.core.common.BuildInfo
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.CozyPalette
import com.nzrbits.hush.core.common.model.DateFormatChoice
import com.nzrbits.hush.core.common.model.FontFamilyChoice
import com.nzrbits.hush.core.common.model.FontScale
import com.nzrbits.hush.core.common.model.GestureAction
import com.nzrbits.hush.core.common.model.PixelScene
import com.nzrbits.hush.core.common.model.SceneDensity
import com.nzrbits.hush.core.common.model.ThemeMode
import com.nzrbits.hush.core.common.model.TimeFormatChoice
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushChoiceRow
import com.nzrbits.hush.core.designsystem.components.HushPrimaryButton
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushScreen
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.components.HushSpacer
import com.nzrbits.hush.core.designsystem.components.HushSwitchRow
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.core.system.permissions.HushPermission
import com.nzrbits.hush.feature.cozy.MascotBubble
import com.nzrbits.hush.feature.cozy.MascotState
import com.nzrbits.hush.feature.cozy.Sayings
import java.time.LocalDateTime

data class SettingsNavigation(
    val back: () -> Unit,
    val limits: () -> Unit,
    val blocking: () -> Unit,
    val schedules: () -> Unit,
    val screenTime: () -> Unit,
    val notificationRules: () -> Unit,
    val notificationLog: () -> Unit,
    val shortVideo: () -> Unit,
    val home: () -> Unit,
    val favorites: () -> Unit,
    val hidden: () -> Unit,
    val appearance: () -> Unit,
    val gestures: () -> Unit,
    val cozy: () -> Unit,
    val permissions: () -> Unit,
    val about: () -> Unit,
    val privacy: () -> Unit,
    val faq: () -> Unit,
    val updates: () -> Unit,
)

/**
 * The RoleManager dialog only works through startActivityForResult, so every "make Hush the
 * default launcher" entry point uses this launcher instead of a plain startActivity.
 */
@Composable
fun rememberDefaultLauncherRequest(viewModel: SettingsViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { viewModel.refreshPermissions() }
    return { runCatching { launcher.launch(viewModel.defaultLauncherIntent()) }.onFailure { viewModel.openPermission(HushPermission.DEFAULT_LAUNCHER) } }
}

/** Root settings screen. Mr. Nook talks at the top when the user allows it. */
@Composable
fun SettingsScreen(nav: SettingsNavigation, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    val requestDefaultLauncher = rememberDefaultLauncherRequest(viewModel)
    HushScreen(title = "Einstellungen", onBack = nav.back) {
        if (settings.appearance.showMascotInSettings) {
            HushSpacer(12)
            MascotBubble(text = Sayings.settings(LocalDateTime.now()), state = MascotState.TALK, mascotSize = 64.dp)
        }
        // Skin switch first: it is the setting people look for.
        HushSectionHeader("Skin")
        HushChoiceRow(ThemeMode.entries, settings.appearance.themeMode, { if (it == ThemeMode.MINIMAL) "Minimal" else "Cozy" }) { v -> viewModel.updateAppearance { it.copy(themeMode = v) } }
        HushRow("Schrift, Hintergrund, Szenen", subtitle = "Größe, Schriftart, Bild, Pixel-Szene", onClick = nav.appearance)

        // Same labels and subtitles as the wellbeing hub, so both lists read as one.
        HushSectionHeader("Digital Wellbeing")
        HushRow("App-Blockierung", subtitle = "Aktive Sperren und Übersicht", onClick = nav.blocking)
        HushRow("Blockierpläne", subtitle = "Wiederkehrende Fokuszeiten", onClick = nav.schedules)
        HushRow("Zeiterinnerungen", subtitle = "Tägliche Limits pro App, nur Erinnerung", onClick = nav.limits)
        HushRow("Bildschirmzeit", subtitle = "Heute und die letzten sieben Tage", onClick = nav.screenTime)
        HushRow("Kurzvideos", subtitle = "Shorts, Reels, Spotlight verlassen", onClick = nav.shortVideo)
        HushRow("Benachrichtigungsfilter", subtitle = "Regeln und gefilterte Meldungen", onClick = nav.notificationLog)

        HushSectionHeader("Homescreen")
        HushRow("Favoriten", onClick = nav.favorites)
        HushRow("Verborgene Apps", onClick = nav.hidden)
        HushRow("Uhr, Datum, Schnellzugriffe", onClick = nav.home)

        HushSectionHeader("Darstellung")
        HushRow("Gesten", onClick = nav.gestures)
        HushRow("Cozy: ${HushConfig.MASCOT_NAME} und Szenen", onClick = nav.cozy)

        HushSectionHeader("Mehr")
        HushRow("Berechtigungen", onClick = nav.permissions)
        HushRow("Standard-Launcher", subtitle = if (viewModel.isDefaultLauncher()) "${HushConfig.APP_NAME} ist Standard" else "Noch nicht Standard", onClick = requestDefaultLauncher)
        HushRow("Datenschutz", onClick = nav.privacy)
        HushRow("FAQ", onClick = nav.faq)
        HushRow("Updates", subtitle = "Optionaler Abruf von GitHub, standardmäßig aus", onClick = nav.updates)
        HushRow("Über ${HushConfig.APP_NAME}", onClick = nav.about)
        HushRow("Launcher verlassen", subtitle = "Anderen Launcher als Standard wählen", onClick = viewModel::leaveLauncher)
        Text(
            "Support: ${HushConfig.SUPPORT_EMAIL}",
            style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
fun HomeSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    HushScreen(title = "Homescreen", onBack = onBack) {
        HushSectionHeader("Schnellzugriffe")
        HushSwitchRow("Telefon", checked = s.home.showPhone, onCheckedChange = { v -> viewModel.updateHome { it.copy(showPhone = v) } })
        HushSwitchRow("Kamera", checked = s.home.showCamera, onCheckedChange = { v -> viewModel.updateHome { it.copy(showCamera = v) } })
        HushSwitchRow("Wecker", checked = s.home.showAlarm, onCheckedChange = { v -> viewModel.updateHome { it.copy(showAlarm = v) } })
        HushSectionHeader("Uhrzeit")
        HushChoiceRow(TimeFormatChoice.entries, s.home.timeFormat, { when (it) { TimeFormatChoice.SYSTEM -> "System"; TimeFormatChoice.H24 -> "24 h"; TimeFormatChoice.H12 -> "12 h" } }) { v -> viewModel.updateHome { it.copy(timeFormat = v) } }
        HushSectionHeader("Datum")
        HushChoiceRow(DateFormatChoice.entries, s.home.dateFormat, { when (it) { DateFormatChoice.LONG -> "Montag, 28. September"; DateFormatChoice.SHORT -> "Mo, 28. Sep"; DateFormatChoice.NUMERIC -> "28.09.2026" } }) { v -> viewModel.updateHome { it.copy(dateFormat = v) } }
        HushSectionHeader("Weiteres")
        HushSwitchRow("Ladeanzeige", subtitle = "Zeigt „Lädt · 80 %“ unter dem Datum", checked = s.home.showChargingAnimation, onCheckedChange = { v -> viewModel.updateHome { it.copy(showChargingAnimation = v) } })
        HushSwitchRow("Tastatur automatisch öffnen", subtitle = "In der App-Suche", checked = s.home.autoKeyboardInDrawer, onCheckedChange = { v -> viewModel.updateHome { it.copy(autoKeyboardInDrawer = v) } })
    }
}

@Composable
fun AppearanceScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::persistWallpaper) }
    HushScreen(title = "Darstellung", onBack = onBack) {
        HushSectionHeader("Modus")
        HushChoiceRow(ThemeMode.entries, s.appearance.themeMode, { if (it == ThemeMode.MINIMAL) "Minimal" else "Cozy" }) { v -> viewModel.updateAppearance { it.copy(themeMode = v) } }
        Text(
            if (s.appearance.themeMode == ThemeMode.MINIMAL) "Schwarz, weiß, eine Textliste. Sonst nichts." else "Creme, Terrakotta, Salbei. ${HushConfig.MASCOT_NAME} redet mit.",
            style = HushTheme.typography.caption, color = colors.muted,
        )
        if (s.appearance.themeMode == ThemeMode.COZY) {
            HushSectionHeader("Cozy-Palette")
            HushChoiceRow(CozyPalette.entries, s.appearance.cozyPalette, { when (it) { CozyPalette.AUTO -> "System"; CozyPalette.LIGHT -> "Hell"; CozyPalette.DARK -> "Dunkel" } }) { v -> viewModel.updateAppearance { it.copy(cozyPalette = v) } }
        }
        HushSectionHeader("Schriftgröße")
        HushChoiceRow(FontScale.entries, s.appearance.fontScale, { it.label }) { v -> viewModel.updateAppearance { it.copy(fontScale = v) } }
        HushSectionHeader("Schriftart")
        HushChoiceRow(FontFamilyChoice.entries, s.appearance.fontFamily, { it.label }) { v -> viewModel.updateAppearance { it.copy(fontFamily = v) } }
        HushSectionHeader("Hintergrundbild")
        HushRow(
            if (s.appearance.wallpaperUri == null) "Bild wählen" else "Bild ändern",
            subtitle = if (s.appearance.wallpaperUri == null) "Standard ist die einfarbige Fläche des Modus" else "Eigenes Bild aktiv",
            onClick = { pickImage.launch(arrayOf("image/*")) },
        )
        if (s.appearance.wallpaperUri != null) HushTextButton("Bild entfernen", onClick = viewModel::clearWallpaper, danger = true)
        HushSectionHeader("Bewegung")
        HushSwitchRow("Animationen reduzieren", subtitle = "Stoppt Szenen und ${HushConfig.MASCOT_NAME}", checked = s.appearance.reduceMotion, onCheckedChange = { v -> viewModel.updateAppearance { it.copy(reduceMotion = v) } })
    }
}

@Composable
fun CozySettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    HushScreen(title = "Cozy", onBack = onBack) {
        HushSpacer(12)
        MascotBubble(text = "Auf dem Startbildschirm bin ich im Minimal Mode still.", state = MascotState.IDLE, mascotSize = 64.dp)
        HushSectionHeader(HushConfig.MASCOT_NAME)
        HushSwitchRow("Auf dem Startbildschirm", subtitle = "Nur im Cozy Mode sichtbar", checked = s.appearance.showMascot, onCheckedChange = { v -> viewModel.updateAppearance { it.copy(showMascot = v) } })
        HushSwitchRow("In den Einstellungen", subtitle = "In beiden Modi", checked = s.appearance.showMascotInSettings, onCheckedChange = { v -> viewModel.updateAppearance { it.copy(showMascotInSettings = v) } })
        HushSectionHeader("Pixel-Szene")
        Text("Ein paar Pixel, die über den Startbildschirm ziehen. In beiden Modi, in Minimal einfarbig grau.", style = HushTheme.typography.caption, color = colors.muted)
        PixelScene.entries.forEach { scene ->
            HushRow(scene.label, trailing = if (s.appearance.scene == scene) "•" else null, onClick = { viewModel.updateAppearance { it.copy(scene = scene) } })
        }
        if (s.appearance.scene != PixelScene.NONE) {
            HushSectionHeader("Dichte")
            HushChoiceRow(SceneDensity.entries, s.appearance.sceneDensity, { it.label }) { v -> viewModel.updateAppearance { it.copy(sceneDensity = v) } }
        }
    }
}

@Composable
fun GesturesScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    val options = GestureAction.entries
    HushScreen(title = "Gesten", onBack = onBack) {
        GestureRow("Nach oben wischen", s.gestures.swipeUp, options) { v -> viewModel.updateGestures { it.copy(swipeUp = v) } }
        GestureRow("Nach unten wischen", s.gestures.swipeDown, options) { v -> viewModel.updateGestures { it.copy(swipeDown = v) } }
        GestureRow("Doppeltippen", s.gestures.doubleTap, options) { v -> viewModel.updateGestures { it.copy(doubleTap = v) } }
        HushSectionHeader("Fest")
        HushRow("Lang drücken auf App", trailing = "Kontextmenü")
        HushRow("Lang drücken auf freie Fläche", trailing = "Einstellungen")
        Text(
            "Bildschirm sperren und Benachrichtigungen öffnen laufen über die Bedienungshilfe von ${HushConfig.APP_NAME}. Sperren braucht Android 9 oder neuer. Ohne die Bedienungshilfe passiert bei diesen beiden Gesten nichts.",
            style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun GestureRow(title: String, current: GestureAction, options: List<GestureAction>, onSelect: (GestureAction) -> Unit) {
    HushSectionHeader(title)
    Column(Modifier.fillMaxWidth()) {
        options.forEach { a ->
            HushRow(a.label, trailing = if (a == current) "•" else null, onClick = { onSelect(a) })
        }
    }
}

@Composable
fun PermissionsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val states by viewModel.permissionStates.collectAsStateWithLifecycle()
    val colors = HushTheme.colors
    val requestDefaultLauncher = rememberDefaultLauncherRequest(viewModel)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.refreshPermissions() }
    }
    HushScreen(title = "Berechtigungen", onBack = onBack) {
        Text(
            "Jede Berechtigung wird erst gebraucht, wenn du die Funktion nutzt. Hier steht, was ${HushConfig.APP_NAME} damit macht.",
            style = HushTheme.typography.caption, color = colors.muted,
        )
        states.forEach { state ->
            val (why, without) = explain(state.permission)
            HushSpacer(12)
            HushCard(onClick = { if (state.permission == HushPermission.DEFAULT_LAUNCHER) requestDefaultLauncher() else viewModel.openPermission(state.permission) }) {
                Column {
                    Text(state.permission.title + if (state.granted) " · erteilt" else " · fehlt", style = HushTheme.typography.body, color = colors.text)
                    Text(why, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
                    Text("Ohne: $without", style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

private fun explain(p: HushPermission): Pair<String, String> = when (p) {
    HushPermission.DEFAULT_LAUNCHER -> "Damit die Home-Taste zu ${HushConfig.APP_NAME} führt." to "Der bisherige Launcher bleibt aktiv."
    HushPermission.USAGE_ACCESS -> "Liest die Android-Nutzungsstatistik: welche App wann im Vordergrund war. Nötig für Bildschirmzeit und Zeiterinnerungen. Bleibt auf dem Gerät." to "Keine Bildschirmzeit, keine Limits."
    HushPermission.NOTIFICATION_ACCESS -> "Liest Titel und Text eingehender Benachrichtigungen, um Filterregeln anzuwenden. Gefilterte Meldungen werden lokal gespeichert, 30 Tage." to "Kein Benachrichtigungsfilter."
    HushPermission.ACCESSIBILITY -> "Erkennt, welche App im Vordergrund ist, schließt gesperrte Apps, verlässt Kurzvideo-Bereiche in vier Apps, sperrt den Bildschirm per Geste. Keine Inhalte werden gespeichert oder gesendet." to "Sperren sind nur Hinweise im Launcher. Kurzvideo-Blockierung und Sperr-Geste gehen nicht."
    HushPermission.POST_NOTIFICATIONS -> "Für die eigenen Zeiterinnerungen." to "Limits werden erreicht, aber nicht gemeldet."
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = HushTheme.colors
    HushScreen(title = "Über ${HushConfig.APP_NAME}", onBack = onBack) {
        Text("${HushConfig.APP_NAME} ist ein ruhiger Launcher mit Fokus-Funktionen.", style = HushTheme.typography.body, color = colors.text)
        HushSpacer(12)
        Text("Version ${BuildInfo.versionName}", style = HushTheme.typography.caption, color = colors.muted)
        HushSpacer(12)
        Text("${HushConfig.MASCOT_NAME} stammt aus der App Mr. Nook. Schriften Nunito und Pixelify Sans unter SIL Open Font License 1.1. Keine Analyse, keine Werbung, Netz nur für den optionalen Update-Abruf.", style = HushTheme.typography.caption, color = colors.muted)
        HushSpacer(12)
        Text("Support: ${HushConfig.SUPPORT_EMAIL}", style = HushTheme.typography.caption, color = colors.muted)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val colors = HushTheme.colors
    HushScreen(title = "Datenschutz", onBack = onBack) {
        listOf(
            "Alles bleibt auf dem Gerät. Die einzige Netzverbindung ist der optionale Update-Abruf von github.com, standardmäßig aus. GitHub sieht dabei deine IP-Adresse, sonst nichts.",
            "Gespeichert werden: Favoriten, Namen, Ordner, Sperren, Pläne, Limits, Filterregeln, gefilterte Benachrichtigungen (30 Tage), Einstellungen.",
            "Nutzungsstatistik wird nur gelesen, wenn du den Nutzungszugriff erteilst, und nicht dauerhaft gespeichert.",
            "Die Bedienungshilfe liest den Namen der App im Vordergrund. Nur für YouTube, Instagram, Facebook und Snapchat werden Oberflächen-Kennungen geprüft, um Kurzvideos zu erkennen. Nichts davon wird gespeichert.",
            "Löschen: App deinstallieren entfernt alle Daten.",
        ).forEach { line ->
            Text("• $line", style = HushTheme.typography.body, color = colors.text, modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

@Composable
fun FaqScreen(onBack: () -> Unit) {
    val colors = HushTheme.colors
    HushScreen(title = "FAQ", onBack = onBack) {
        listOf(
            "Warum öffnet sich eine gesperrte App trotzdem?" to "Ohne Bedienungshilfe kann ${HushConfig.APP_NAME} sie nur im eigenen Startbildschirm ausgrauen. Mit Bedienungshilfe wird sie geschlossen, sobald sie erscheint. Ein kurzes Aufblitzen ist normal.",
            "Kann ich die Sperre umgehen?" to "Ja: Bedienungshilfe ausschalten oder ${HushConfig.APP_NAME} deinstallieren. Es ist eine Bremse für dich, kein Kinderschutz.",
            "Warum spielt trotzdem ein Ton bei gefilterten Benachrichtigungen?" to "Android liefert die Meldung erst zu, dann kann ${HushConfig.APP_NAME} sie entfernen. Für Stille die Kanäle in den Android-Einstellungen stumm schalten.",
            "Kurzvideo-Blockierung geht nicht mehr." to "Die App hat ihre Oberfläche geändert. Die Erkennung nutzt Oberflächen-Kennungen und muss dann aktualisiert werden.",
            "Wo ist der Unterschied zwischen Erinnerung und Sperre?" to "Zeiterinnerung: Benachrichtigung bei 80 % und 100 % des Limits, sonst nichts. Sperre: App wird geschlossen, bis die Zeit um ist.",
            "Was passiert nach einem Neustart?" to "Alles bleibt: Favoriten, Sperren, Pläne. Der Neustart verlängert oder verkürzt keine Sperre.",
        ).forEach { (q, a) ->
            Text(q, style = HushTheme.typography.body, color = colors.text, modifier = Modifier.padding(top = 14.dp))
            Text(a, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** First start: three steps, nothing mandatory. */
@Composable
fun OnboardingScreen(onDone: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val requestDefaultLauncher = rememberDefaultLauncherRequest(viewModel)
    HushScreen(title = "Hallo.") {
        HushSpacer(12)
        MascotBubble(text = "Ich bin ${HushConfig.MASCOT_NAME}. Auf dem Startbildschirm rede ich nur im Cozy Mode.", state = MascotState.TALK, mascotSize = 64.dp)
        HushSpacer(16)
        Text("${HushConfig.APP_NAME} ist ein ruhiger Startbildschirm: Uhr, Datum, deine Apps als Text. Dazu Sperren, Zeitlimits und Benachrichtigungsfilter, wenn du sie willst.", style = HushTheme.typography.body, color = colors.text)
        HushSpacer(16)
        HushCard {
            Column {
                Text("1. Als Standard-Launcher setzen", style = HushTheme.typography.body, color = colors.text)
                Text("Damit die Home-Taste hierher führt. Jederzeit umkehrbar.", style = HushTheme.typography.caption, color = colors.muted)
                HushSpacer(8)
                HushPrimaryButton(if (viewModel.isDefaultLauncher()) "Erledigt" else "Standard-Launcher wählen", onClick = requestDefaultLauncher)
            }
        }
        HushSpacer(12)
        HushCard {
            Column {
                Text("2. Berechtigungen später", style = HushTheme.typography.body, color = colors.text)
                Text("Nutzungszugriff, Benachrichtigungen und Bedienungshilfe fragt ${HushConfig.APP_NAME} erst, wenn du die Funktion öffnest.", style = HushTheme.typography.caption, color = colors.muted)
            }
        }
        HushSpacer(12)
        HushCard {
            Column {
                Text("3. Favoriten", style = HushTheme.typography.body, color = colors.text)
                Text("Nach oben wischen, App lang drücken, „Zu Favoriten hinzufügen“.", style = HushTheme.typography.caption, color = colors.muted)
            }
        }
        HushSpacer(24)
        HushPrimaryButton("Los", onClick = { viewModel.setOnboardingDone(); onDone() })
    }
}
