package com.nzrbits.hush.feature.notifications.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nzrbits.hush.core.common.model.CapturedNotification
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.feature.notifications.data.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures notifications that match an enabled rule: stores title/text locally and cancels
 * the notification so it leaves the shade. Ongoing (foreground service, media) and group
 * summary notifications are never touched, because cancelling those breaks other apps.
 *
 * Android limitation: a listener can only cancel a notification after it was posted, so a
 * sound or vibration may already have played. That is stated in the UI.
 */
@AndroidEntryPoint
class HushNotificationListener : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var apps: AppsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (n.packageName == packageName) return
        if (n.isOngoing) return
        val flags = n.notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return

        scope.launch {
            val rule = repository.evaluate(n.packageName) ?: return@launch
            val extras = n.notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""
            if (title.isBlank() && text.isBlank()) return@launch
            repository.capture(
                CapturedNotification(
                    packageName = n.packageName,
                    appLabel = apps.labelFor(n.packageName),
                    title = title,
                    text = text,
                    postedAtMillis = n.postTime,
                    ruleId = rule.id,
                ),
            )
            runCatching { cancelNotification(n.key) }
        }
    }
}
