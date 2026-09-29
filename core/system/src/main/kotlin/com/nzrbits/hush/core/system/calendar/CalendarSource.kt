package com.nzrbits.hush.core.system.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.time.HushClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class NextEvent(
    val eventId: Long,
    val title: String,
    val beginMillis: Long,
    val allDay: Boolean,
)

/**
 * Reads the next upcoming event from the device calendar provider. Only calendars of Google
 * accounts are considered for now (account type "com.google"), as agreed for the first test.
 * Needs the runtime permission READ_CALENDAR; without it every call returns null.
 */
@Singleton
class CalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Next event that has not ended yet, within the next [daysAhead] days. */
    suspend fun nextEvent(daysAhead: Int = 7): NextEvent? = withContext(dispatchers.io) {
        if (!hasPermission()) return@withContext null
        val now = clock.now().toEpochMilli()
        val end = now + daysAhead * 24L * 60 * 60 * 1000
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(end.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Instances.VISIBLE} = 1 AND ${CalendarContract.Instances.END} > ?"
        val args = arrayOf("com.google", now.toString())
        runCatching {
            context.contentResolver.query(uri, projection, selection, args, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                if (!c.moveToFirst()) return@use null
                NextEvent(
                    eventId = c.getLong(0),
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "Termin",
                    beginMillis = c.getLong(2),
                    allDay = c.getInt(4) == 1,
                )
            }
        }.getOrNull()
    }

    /** Opens the event in the calendar app. */
    fun openEvent(event: NextEvent): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
