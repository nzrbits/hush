package com.nzrbits.hush.core.system.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.AppUsage
import com.nzrbits.hush.core.common.model.DailyUsage
import com.nzrbits.hush.core.common.time.HushClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads UsageStatsManager events and aggregates them per local day. Requires Usage Access. */
@Singleton
class UsageStatsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val selfPackage = context.packageName

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), selfPackage)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), selfPackage)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground time per app between two epoch milliseconds. Empty without permission. */
    suspend fun usageBetween(startMillis: Long, endMillis: Long): List<AppUsage> = withContext(dispatchers.io) {
        if (!hasUsageAccess()) return@withContext emptyList()
        val events = ArrayList<ForegroundEvent>()
        // Query a little earlier so an app already open at window start is accounted for.
        val query = usageStatsManager.queryEvents(startMillis - 6 * 60 * 60 * 1000L, endMillis)
        val event = UsageEvents.Event()
        while (query.getNextEvent(event)) {
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> ForegroundEvent.Kind.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundEvent.Kind.PAUSED
                else -> null
            } ?: continue
            if (event.packageName == selfPackage) continue
            events += ForegroundEvent(event.packageName, event.timeStamp, kind)
        }
        UsageAggregator.aggregate(events, startMillis, endMillis)
    }

    suspend fun today(): DailyUsage {
        val start = clock.startOfDayMillis()
        val now = clock.now().toEpochMilli()
        val perApp = usageBetween(start, now)
        return DailyUsage(start, perApp.sumOf { it.foregroundMillis }, perApp)
    }

    /** Last [days] local days including today, oldest first. */
    suspend fun lastDays(days: Int): List<DailyUsage> {
        val today = clock.today()
        val now = clock.now().toEpochMilli()
        return (days - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val start = clock.startOfDayMillis(date)
            val end = if (back == 0) now else clock.startOfDayMillis(date.plusDays(1))
            val perApp = usageBetween(start, end)
            DailyUsage(start, perApp.sumOf { it.foregroundMillis }, perApp)
        }
    }

    suspend fun todayForPackage(packageName: String): Long =
        today().perApp.firstOrNull { it.packageName == packageName }?.foregroundMillis ?: 0L
}
