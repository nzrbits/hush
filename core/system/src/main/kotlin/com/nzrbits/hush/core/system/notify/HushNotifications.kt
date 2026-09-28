package com.nzrbits.hush.core.system.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nzrbits.hush.core.common.HushConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Hush's own notifications: usage limit reminders and warnings. Low priority, no sound. */
@Singleton
class HushNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    companion object {
        const val CHANNEL_LIMITS = "hush.limits"
        const val ID_LIMIT_BASE = 4000
    }

    fun ensureChannels() {
        val channel = NotificationChannel(CHANNEL_LIMITS, "${HushConfig.APP_NAME}: Zeiterinnerungen", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Erinnerungen, wenn ein selbst gesetztes Tageslimit erreicht ist."
            setSound(null, null)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    /** Returns true only when the notification was handed to the system. */
    fun showLimitReminder(packageName: String, appLabel: String, minutesUsed: Long, limitMinutes: Int, warning: Boolean, tapIntent: Intent?): Boolean {
        if (!canNotify()) return false
        ensureChannels()
        val title = if (warning) "$appLabel: fast am Limit" else "$appLabel: Limit erreicht"
        val text = if (warning) {
            "$minutesUsed von $limitMinutes Minuten heute. Vielleicht ein guter Moment für eine Pause."
        } else {
            "$minutesUsed Minuten heute, dein Limit sind $limitMinutes. Das ist eine Erinnerung, keine Sperre."
        }
        val pending = tapIntent?.let {
            PendingIntent.getActivity(context, packageName.hashCode(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_LIMITS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return runCatching { manager.notify(ID_LIMIT_BASE + (packageName.hashCode() and 0xFFFF), notification) }.isSuccess
    }
}
