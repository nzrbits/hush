package com.nzrbits.hush.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nzrbits.hush.core.common.model.LauncherApp
import com.nzrbits.hush.core.common.text.Search
import com.nzrbits.hush.core.designsystem.components.HushAppItem
import com.nzrbits.hush.core.designsystem.components.HushEmptyState
import com.nzrbits.hush.core.designsystem.components.HushSectionHeader
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import kotlinx.coroutines.launch

/**
 * App drawer: search field on top, alphabetical list with folders, letter index on the right.
 */
@Composable
fun DrawerScreen(
    navigation: AppActionsNavigation,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: DrawerViewModel = hiltViewModel(),
) {
    val colors = HushTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<LauncherApp?>(null) }

    LaunchedEffect(state.autoKeyboard) {
        if (state.autoKeyboard == true) runCatching { focusRequester.requestFocus() }
    }

    // Flattened items with stable index positions for the letter index.
    val items = remember(state.sections) {
        buildList<DrawerItem> {
            state.sections.forEach { section ->
                if (section.title != null) add(DrawerItem.Header(section.title))
                section.apps.forEach { add(DrawerItem.App(it, section.folderId == null)) }
            }
        }
    }
    val letterPositions = remember(items) {
        val map = LinkedHashMap<Char, Int>()
        items.forEachIndexed { index, item ->
            if (item is DrawerItem.App && item.loose) {
                val letter = Search.indexLetter(item.app.displayLabel)
                if (!map.containsKey(letter)) map[letter] = index
            }
        }
        map
    }

    val closeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            // Swipe to the right (from the left) closes the drawer, mirroring how it was opened.
            .pointerInput(Unit) {
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx > closeThresholdPx) { viewModel.clearQuery(); onClose() } },
                    onHorizontalDrag = { change, amount -> change.consume(); dx += amount },
                )
            },
    ) {
        BasicTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            singleLine = true,
            textStyle = HushTheme.typography.listItem.copy(color = colors.text),
            cursorBrush = SolidColor(colors.text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (viewModel.launchFirstResult()) { viewModel.clearQuery(); onClose() } }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box {
                    if (state.query.isEmpty()) {
                        Text("Suchen …", style = HushTheme.typography.listItem, color = colors.muted)
                    }
                    inner()
                }
            },
        )
        Row(Modifier.fillMaxSize()) {
            if (!state.loaded) {
                // First list not in yet: show nothing rather than a wrong empty state.
            } else if (items.none { it is DrawerItem.App }) {
                HushEmptyState(
                    if (state.totalApps == 0) "Keine Apps gefunden. Läuft ${com.nzrbits.hush.core.common.HushConfig.APP_NAME} als Launcher?" else "Nichts gefunden.",
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 4.dp, bottom = 48.dp),
                ) {
                    if (state.query.isEmpty()) {
                        item(key = "hush.settings") {
                            HushAppItem(label = "Einstellungen", dimmed = true, onClick = { onOpenSettings() })
                        }
                    }
                    items.forEach { item ->
                        when (item) {
                            is DrawerItem.Header -> item { HushSectionHeader(item.title) }
                            is DrawerItem.App -> item(key = item.app.key.id + item.app.activityClassName) {
                                val blocked = item.app.packageName in state.blockedPackages
                                HushAppItem(
                                    label = item.app.displayLabel,
                                    dimmed = blocked,
                                    secondary = when {
                                        blocked -> "gesperrt"
                                        item.app.isWorkProfile -> "Arbeit"
                                        else -> null
                                    },
                                    onClick = { viewModel.launch(item.app); viewModel.clearQuery(); onClose() },
                                    onLongClick = { selected = item.app },
                                )
                            }
                        }
                    }
                }
                if (state.letters.isNotEmpty()) {
                    LetterIndex(
                        letters = state.letters,
                        onLetter = { letter ->
                            letterPositions[letter]?.let { index -> scope.launch { listState.scrollToItem(index) } }
                        },
                    )
                }
            }
        }
    }

    selected?.let { app ->
        AppActionsSheet(app = app, navigation = navigation, onDismiss = { selected = null })
    }
}

private sealed interface DrawerItem {
    data class Header(val title: String) : DrawerItem
    data class App(val app: LauncherApp, val loose: Boolean) : DrawerItem
}

/** Vertical A–Z strip. Dragging over it jumps the list, so it works with one thumb. */
@Composable
private fun LetterIndex(letters: List<Char>, onLetter: (Char) -> Unit) {
    val colors = HushTheme.colors
    var height by remember { mutableStateOf(1) }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(44.dp)
            .padding(end = 4.dp)
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> letterAt(letters, offset.y, height)?.let(onLetter) },
                    onVerticalDrag = { change, _ -> change.consume(); letterAt(letters, change.position.y, height)?.let(onLetter) },
                )
            },
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            Text(
                letter.toString(),
                style = HushTheme.typography.caption,
                color = colors.muted,
                modifier = Modifier.pointerInput(letter) {
                    detectTapGestures(onTap = { onLetter(letter) })
                },
            )
        }
    }
}

private fun letterAt(letters: List<Char>, y: Float, height: Int): Char? {
    if (letters.isEmpty()) return null
    val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.size - 1)
    return letters[index]
}
