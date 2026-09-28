package com.nzrbits.hush.feature.wellbeing.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nzrbits.hush.core.common.model.BlockReason
import com.nzrbits.hush.core.designsystem.components.HushCard
import com.nzrbits.hush.core.designsystem.components.HushSpacer
import com.nzrbits.hush.core.designsystem.components.HushTextButton
import com.nzrbits.hush.core.designsystem.theme.HushTheme
import com.nzrbits.hush.feature.cozy.MascotBubble
import com.nzrbits.hush.feature.cozy.MascotState
import com.nzrbits.hush.feature.cozy.Sayings
import com.nzrbits.hush.feature.wellbeing.data.BlockedEvent
import java.time.LocalDateTime

/**
 * Shown on the home screen right after the accessibility service closed a blocked app.
 * In Cozy Mode Mr. Nook says it, in Minimal Mode it is one line of text.
 */
@Composable
fun BlockedNotice(event: BlockedEvent, appLabel: String, showMascot: Boolean, onDismiss: () -> Unit, onOpenWellbeing: () -> Unit) {
    val colors = HushTheme.colors
    val until = when (val r = event.reason) {
        is BlockReason.Manual -> formatEnd(r.block.endsAtMillis)
        is BlockReason.Scheduled -> "${formatEnd(r.endsAtMillis)} (${r.schedule.name})"
    }
    HushCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (showMascot) {
                MascotBubble(text = Sayings.blocked(LocalDateTime.now()), state = MascotState.SLEEP, mascotSize = 64.dp)
                HushSpacer(8)
            }
            Text("$appLabel ist gesperrt bis $until.", style = HushTheme.typography.body, color = colors.text)
            HushSpacer(4)
            androidx.compose.foundation.layout.Row {
                HushTextButton("Okay", onClick = onDismiss)
                HushTextButton("Sperren ansehen", onClick = onOpenWellbeing)
            }
        }
    }
}
