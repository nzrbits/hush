package com.nzrbits.hush.feature.wellbeing.data

import android.content.Context
import android.content.Intent
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.UsageLimit
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.database.UsageLimitDao
import com.nzrbits.hush.core.database.UsageLimitEntity
import com.nzrbits.hush.core.database.toModel
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.apps.AppsRepository
import com.nzrbits.hush.core.system.notify.HushNotifications
import com.nzrbits.hush.core.system.usage.UsageStatsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Pure decision for reminders, unit tested separately. */
object LimitDecision {
    enum class Outcome { NONE, WARN, REMIND }

    fun decide(usedMinutes: Long, limit: UsageLimit, remindedToday: Boolean, warnedToday: Boolean): Outcome {
        if (usedMinutes >= limit.dailyLimitMinutes) return if (remindedToday) Outcome.NONE else Outcome.REMIND
        val warnAt = limit.dailyLimitMinutes * limit.warnAtPercent / 100
        if (limit.warnAtPercent in 1..99 && usedMinutes >= warnAt) return if (warnedToday) Outcome.NONE else Outcome.WARN
        return Outcome.NONE
    }
}

@Singleton
class UsageLimitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UsageLimitDao,
    private val usage: UsageStatsSource,
    private val apps: AppsRepository,
    private val notifications: HushNotifications,
    private val settings: SettingsRepository,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    val limits: Flow<List<UsageLimit>> = dao.observeAll().map { it.map(UsageLimitEntity::toModel) }

    suspend fun get(packageName: String): UsageLimit? = withContext(dispatchers.io) { dao.get(packageName)?.toModel() }

    suspend fun set(limit: UsageLimit) = withContext(dispatchers.io) {
        val existing = dao.get(limit.packageName)
        dao.upsert(
            UsageLimitEntity(
                packageName = limit.packageName,
                dailyLimitMinutes = limit.dailyLimitMinutes,
                warnAtPercent = limit.warnAtPercent,
                lastReminderEpochDay = existing?.lastReminderEpochDay,
                lastWarningEpochDay = existing?.lastWarningEpochDay,
            ),
        )
    }

    suspend fun remove(packageName: String) = withContext(dispatchers.io) { dao.delete(packageName) }

    /**
     * Checks one package against its limit and posts a reminder or warning at most once per
     * local day. Called by the accessibility service when the app comes to the foreground and
     * by a periodic worker as a fallback.
     */
    suspend fun check(packageName: String) = withContext(dispatchers.io) {
        if (!settings.current().wellbeing.usageLimitRemindersEnabled) return@withContext
        if (!usage.hasUsageAccess() || !notifications.canNotify()) return@withContext
        val entity = dao.get(packageName) ?: return@withContext
        checkEntity(entity, usage.todayForPackage(packageName))
    }

    /** One usage query for all limits, used by the periodic worker. */
    suspend fun checkAll() = withContext(dispatchers.io) {
        if (!settings.current().wellbeing.usageLimitRemindersEnabled) return@withContext
        if (!usage.hasUsageAccess() || !notifications.canNotify()) return@withContext
        val all = dao.all()
        if (all.isEmpty()) return@withContext
        val today = usage.today()
        all.forEach { entity ->
            checkEntity(entity, today.perApp.firstOrNull { it.packageName == entity.packageName }?.foregroundMillis ?: 0L)
        }
    }

    /** Marks the day only after the notification was actually posted, so a denied permission does not eat the reminder. */
    private suspend fun checkEntity(entity: UsageLimitEntity, usedMillis: Long) {
        val today = clock.today().toEpochDay()
        val usedMinutes = usedMillis / 60_000
        val outcome = LimitDecision.decide(
            usedMinutes, entity.toModel(),
            remindedToday = entity.lastReminderEpochDay == today,
            warnedToday = entity.lastWarningEpochDay == today,
        )
        if (outcome == LimitDecision.Outcome.NONE) return
        val tap = context.packageManager.getLaunchIntentForPackage(context.packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val warning = outcome == LimitDecision.Outcome.WARN
        val shown = notifications.showLimitReminder(entity.packageName, apps.labelFor(entity.packageName), usedMinutes, entity.dailyLimitMinutes, warning, tap)
        if (!shown) return
        if (warning) dao.markWarned(entity.packageName, today) else dao.markReminded(entity.packageName, today)
    }
}
