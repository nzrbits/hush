package com.nzrbits.hush.feature.notifications.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.nzrbits.hush.core.common.model.CapturedNotification
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.apps.ProtectedPackages
import com.nzrbits.hush.feature.notifications.data.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures notifications that match an enabled rule: cancels the notification so it leaves
 * the shade, then stores title/text locally. Never touched, whatever the rules say:
 * ongoing, foreground-service and group-summary notifications, media sessions, calls,
 * alarms, navigation, and anything from protected packages (system, dialer, SMS, ...).
 *
 * Android limitation: a listener can only cancel a notification after it was posted, so a
 * sound or vibration may already have played. That is stated in the UI.
 */
@AndroidEntryPoint
class HushNotificationListener : NotificationListenerService() {

    @Inject lateinit var repository: NotificationRepository
    @Inject lateinit var apps: AppsRepository
    @Inject lateinit var protectedPackages: ProtectedPackages

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> Log.w(TAG, "listener error", e) })

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (n.packageName == packageName) return
        if (n.isOngoing) return
        val notification = n.notification
        val flags = notification.flags
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return
        if (notification.extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true) return
        if (notification.category in untouchableCategories) return

        scope.launch {
            if (protectedPackages.isProtected(n.packageName)) return@launch
            val rule = repository.evaluate(n.packageName) ?: return@launch
            val extras = notification.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: ""
            if (title.isBlank() && text.isBlank()) return@launch
            // Cancel first; only log what actually left the shade, so nothing shows twice.
            val cancelled = runCatching { cancelNotification(n.key) }.isSuccess
            if (!cancelled) return@launch
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
        }
    }

    private companion object {
        const val TAG = "HushNotif"
        val untouchableCategories = setOf(
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SYSTEM,
        )
    }
}
