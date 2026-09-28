package com.nzrbits.hush.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.designsystem.theme.HushTheme

/**
 * Standard screen: status bar padding, optional back arrow, title, scrolling content.
 * Every non-home screen in Hush uses this so spacing and colours stay uniform.
 */
@Composable
fun HushScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScopeActions.() -> Unit = {},
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    content: @Composable () -> Unit,
) {
    val colors = HushTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (onBack != null) 8.dp else 24.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = colors.text)
                }
            }
            Text(
                text = title,
                style = HushTheme.typography.title,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            RowScopeActions.actions()
        }
        val inner = Modifier
            .fillMaxSize()
            .padding(contentPadding)
        if (scrollable) {
            Column(modifier = inner.verticalScroll(rememberScrollState())) {
                content()
                Spacer(Modifier.height(32.dp))
            }
        } else {
            Box(modifier = inner) { content() }
        }
    }
}

/** Marker receiver for top bar actions. */
object RowScopeActions

@Composable
fun HushSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = HushTheme.typography.section,
        color = HushTheme.colors.muted,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
fun HushDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(HushTheme.colors.line),
    )
}

@Composable
fun HushSpacer(height: Int = 12) = Spacer(Modifier.height(height.dp))

@Composable
fun HushHSpacer(width: Int = 12) = Spacer(Modifier.width(width.dp))
