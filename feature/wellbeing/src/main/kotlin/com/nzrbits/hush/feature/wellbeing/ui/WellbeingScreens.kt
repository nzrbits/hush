package com.nzrbits.hush.feature.wellbeing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.time.Durations
import com.nzrbits.hush.core.designsystem.components.DurationSlider
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushDivider
import com.nzrbits.hush.core.designsystem.components.HushEmptyState
import com.nzrbits.hush.core.designsystem.components.HushPrimaryButton
import com.nzrbits.hush.core.designsystem.components.HushRow
import com.nzrbits.hush.core.designsystem.components.HushScreen
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.components.HushSpacer
import com.nzrbits.hush.core.designsystem.components.HushSwitchRow
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.components.SlideToConfirm
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.core.system.permissions.HushPermission
import com.nzrbits.hush.core.system.permissions.PermissionsChecker
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.GERMAN)
private val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)

fun formatEnd(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFmt)

/** Wellbeing hub: today's screen time, active blocks, links to sub features. */
@Composable
fun WellbeingHubScreen(
    onBack: () -> Unit,
    onOpenSchedules: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenShortVideo: () -> Unit,
    onOpenNotificationRules: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: WellbeingHubViewModel = hiltViewModel(),
) {
    val colors = HushTheme.colors
    val status by viewModel.status.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    HushScreen(title = "Fokus & Bildschirmzeit", onBack = onBack) {
        HushCard(onClick = onOpenScreenTime) {
            Column {
                Text("Heute", style = HushTheme.typography.section, color = colors.muted)
                Text(
                    usage.today?.let { Durations.formatMillisShort(it.totalMillis) } ?: "Kein Nutzungszugriff",
                    style = HushTheme.typography.title,
                    color = colors.text,
                )
                usage.today?.top(usage.labels, 3)?.forEach { (label, u) ->
                    Text("$label · ${Durations.formatMillisShort(u.foregroundMillis)}", style = HushTheme.typography.caption, color = colors.muted)
                }
            }
        }
        if (!viewModel.accessibilityOn) {
            HushSpacer(12)
            HushCard(onClick = onOpenPermissions) {
                Text(
                    "Sperren wirken erst, wenn die Bedienungshilfe von ${HushConfig.APP_NAME} eingeschaltet ist. Ohne sie sind Blockierungen nur Erinnerungen im Launcher.",
                    style = HushTheme.typography.caption,
                    color = colors.text,
                )
            }
        }

        HushSectionHeader("Aktive Sperren")
        if (!status.anyActive) HushEmptyState("Gerade ist nichts gesperrt.")
        status.manual.forEach { block ->
            HushRow(
                title = viewModel.labelFor(block.packageName),
                subtitle = "bis ${formatEnd(block.endsAtMillis)}",
                trailingContent = { HushTextButton("Aufheben", onClick = { viewModel.unblock(block.id) }, danger = true) },
            )
        }
        status.scheduled.forEach { (schedule, end) ->
            HushRow(
                title = schedule.name,
                subtitle = "Plan · bis ${formatEnd(end)} · ${schedule.packageNames.size} Apps",
                onClick = onOpenSchedules,
            )
        }

        HushSectionHeader("Bereiche")
        HushRow("Blockierpläne", subtitle = "Wiederkehrende Fokuszeiten", onClick = onOpenSchedules)
        HushRow("Zeiterinnerungen", subtitle = "Tägliche Limits pro App, nur Erinnerung", onClick = onOpenLimits)
        HushRow("Bildschirmzeit", subtitle = "Heute und die letzten sieben Tage", onClick = onOpenScreenTime)
        HushRow("Kurzvideos", subtitle = "Shorts, Reels, Spotlight verlassen", onClick = onOpenShortVideo)
        HushRow("Benachrichtigungsfilter", subtitle = "Regeln und gefilterte Meldungen", onClick = onOpenNotificationRules)
    }
}

/** Block a single app for 1 h to 30 d. Shows usage for context, confirms with a slide. */
@Composable
fun BlockAppScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: BlockAppViewModel = hiltViewModel(),
) {
    val colors = HushTheme.colors
    val minutes by viewModel.minutes.collectAsStateWithLifecycle()
    val today by viewModel.todayMillis.collectAsStateWithLifecycle()
    val week by viewModel.weekMillis.collectAsStateWithLifecycle()
    val active by viewModel.activeBlock.collectAsStateWithLifecycle()

    HushScreen(title = viewModel.label, onBack = onBack) {
        if (viewModel.hasUsageAccess) {
            HushCard {
                Column {
                    Text("Heute", style = HushTheme.typography.section, color = colors.muted)
                    Text(Durations.formatMillisShort(today ?: 0L), style = HushTheme.typography.title, color = colors.text)
                    if (week.isNotEmpty()) {
                        // Bars only when there is something to show; an empty chart is decoration.
                        if ((week.maxOrNull() ?: 0L) >= 60_000L) {
                            HushSpacer(8)
                            UsageBars(week, colors.accent)
                        }
                        Text("Letzte 7 Tage · ${Durations.formatMillisShort(week.sum())}", style = HushTheme.typography.caption, color = colors.muted)
                    }
                }
            }
        } else {
            HushCard(onClick = onOpenPermissions) {
                Text("Nutzungszugriff fehlt. Ohne ihn zeigt ${HushConfig.APP_NAME} hier keine Bildschirmzeit.", style = HushTheme.typography.caption, color = colors.text)
            }
        }

        active?.let { block ->
            HushSectionHeader("Aktive Sperre")
            HushRow(
                title = "Gesperrt bis ${formatEnd(block.endsAtMillis)}",
                trailingContent = { HushTextButton("Aufheben", onClick = { viewModel.unblock(block.id) }, danger = true) },
            )
        }

        if (viewModel.isProtected) {
            HushSectionHeader("Nicht sperrbar")
            Text(
                "Telefon, Einstellungen, Wecker, Tastaturen und System-Apps sperrt ${HushConfig.APP_NAME} nicht. Sonst kämst du an Anrufe, Wecker oder die Bedienungshilfe nicht mehr heran.",
                style = HushTheme.typography.body, color = colors.muted,
            )
            return@HushScreen
        }
        HushSectionHeader(if (active == null) "Blockieren für" else "Neu setzen")
        DurationSlider(
            minutes = minutes,
            minMinutes = HushConfig.MIN_BLOCK_MINUTES,
            maxMinutes = HushConfig.MAX_BLOCK_MINUTES,
            onChange = viewModel::setMinutes,
        )
        HushSpacer(8)
        SlideToConfirm(text = "Zum Bestätigen schieben", onConfirmed = { viewModel.confirm(onBack) })
        HushSpacer(16)
        Text(
            if (viewModel.accessibilityOn) {
                "Die Bedienungshilfe ist an: ${HushConfig.APP_NAME} schließt die App, sobald sie geöffnet wird, auch über andere Wege."
            } else {
                "Bedienungshilfe ist aus: die App wird nur im Launcher als gesperrt gezeigt. Über andere Wege lässt sie sich weiter öffnen."
            },
            style = HushTheme.typography.caption,
            color = colors.muted,
        )
        if (!viewModel.accessibilityOn) {
            HushSpacer(8)
            HushTextButton("Bedienungshilfe einrichten", onClick = onOpenPermissions)
        }
    }
}

/** Simple bar row, one bar per day, a 1 dp baseline, no axis. The numbers are in the text next to it. */
@Composable
fun UsageBars(values: List<Long>, color: androidx.compose.ui.graphics.Color, labels: List<String> = emptyList()) {
    val colors = HushTheme.colors
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val radius = HushTheme.shapes.small.coerceAtMost(4.dp)
    val barShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = radius, topEnd = radius)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.Bottom) {
            values.forEach { v ->
                Box(Modifier.weight(1f).padding(horizontal = 3.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((56 * v.toFloat() / max).dp.coerceAtLeast(3.dp))
                            .background(color, barShape),
                    )
                }
            }
        }
        HushDivider()
        if (labels.size == values.size) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                labels.forEach { label ->
                    Text(label, style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ScreenTimeScreen(onBack: () -> Unit, onOpenPermissions: () -> Unit, viewModel: WellbeingHubViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    HushScreen(title = "Bildschirmzeit", onBack = onBack) {
        if (!usage.hasUsageAccess) {
            HushCard(onClick = onOpenPermissions) {
                Column {
                    Text("Nutzungszugriff fehlt", style = HushTheme.typography.body, color = colors.text)
                    Text(
                        "${HushConfig.APP_NAME} liest dafür die Android-Nutzungsstatistik: welche App wann im Vordergrund war. Die Daten bleiben auf dem Gerät.",
                        style = HushTheme.typography.caption, color = colors.muted,
                    )
                    HushSpacer(8)
                    HushPrimaryButton("Zugriff erlauben", onClick = onOpenPermissions)
                }
            }
            return@HushScreen
        }
        val today = usage.today
        Text(today?.let { Durations.formatMillisShort(it.totalMillis) } ?: "–", style = HushTheme.typography.hero, color = colors.text)
        Text("heute", style = HushTheme.typography.body, color = colors.muted)

        if (usage.week.isNotEmpty()) {
            HushSectionHeader("Letzte 7 Tage")
            UsageBars(
                values = usage.week.map { it.totalMillis },
                color = colors.accent,
                labels = usage.week.map { Instant.ofEpochMilli(it.dayStartMillis).atZone(ZoneId.systemDefault()).format(dayFmt) },
            )
            val avg = usage.week.map { it.totalMillis }.average().toLong()
            Text("Durchschnitt ${Durations.formatMillisShort(avg)} pro Tag", style = HushTheme.typography.caption, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        }

        HushSectionHeader("Heute pro App")
        if (today == null || today.perApp.isEmpty()) HushEmptyState("Noch keine Nutzung heute.")
        today?.top(usage.labels, 15)?.forEach { (label, u) ->
            HushRow(label, trailing = Durations.formatMillisShort(u.foregroundMillis), subtitle = "${u.launchCount}× geöffnet")
        }

        val weekTotals = usage.week.flatMap { it.perApp }.groupBy { it.packageName }.mapValues { e -> e.value.sumOf { it.foregroundMillis } }
            .entries.sortedByDescending { it.value }.take(10)
        if (weekTotals.isNotEmpty()) {
            HushSectionHeader("Häufig genutzt, 7 Tage")
            weekTotals.forEach { (pkg, millis) ->
                HushRow(usage.labels[pkg] ?: pkg, trailing = Durations.formatMillisShort(millis))
            }
        }
    }
}

@Composable
fun ShortVideoScreen(onBack: () -> Unit, onOpenPermissions: () -> Unit, viewModel: ShortVideoViewModel = hiltViewModel()) {
    val colors = HushTheme.colors
    val wellbeing by viewModel.wellbeing.collectAsStateWithLifecycle()
    HushScreen(title = "Kurzvideos", onBack = onBack) {
        HushSwitchRow(
            title = "Kurzvideo-Bereiche verlassen",
            subtitle = "Sobald ein Shorts-, Reels- oder Spotlight-Bereich erkannt wird, drückt ${HushConfig.APP_NAME} Zurück.",
            checked = wellbeing.shortVideoBlockingEnabled,
            onCheckedChange = viewModel::setEnabled,
        )
        // Platform rows appear only once the feature is on (progressive disclosure).
        if (wellbeing.shortVideoBlockingEnabled) {
            HushSectionHeader("Plattformen")
            com.nzrbits.hush.core.common.model.ShortVideoPlatform.entries.forEach { platform ->
                HushSwitchRow(
                    title = platform.displayName,
                    checked = platform in wellbeing.shortVideoPlatforms,
                    onCheckedChange = { viewModel.togglePlatform(platform, it) },
                )
            }
        }
        HushSectionHeader("So funktioniert es")
        Text(
            "Die Erkennung nutzt die Bedienungshilfe und sucht nach bekannten Oberflächen-Kennungen der vier Apps. " +
                "Wenn eine App ihre Oberfläche ändert, kann die Erkennung ausfallen, bis ${HushConfig.APP_NAME} aktualisiert wird. " +
                "Es ist eine Bremse, keine Garantie. Facebook verschleiert seine Kennungen, dort greift nur der Reels-Tab.",
            style = HushTheme.typography.caption, color = colors.muted,
        )
        if (!viewModel.accessibilityOn) {
            HushSpacer(12)
            HushCard(onClick = onOpenPermissions) {
                Text("Bedienungshilfe ist aus. Ohne sie passiert hier nichts.", style = HushTheme.typography.caption, color = colors.text)
            }
        }
    }
}

/** Opens the system screen for a permission from a composable. */
@Composable
fun rememberOpenPermission(checker: PermissionsChecker): (HushPermission) -> Unit {
    val context = LocalContext.current
    return { p -> runCatching { context.startActivity(checker.settingsIntent(p)) } }
}
