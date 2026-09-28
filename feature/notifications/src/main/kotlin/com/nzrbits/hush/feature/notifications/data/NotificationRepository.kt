package com.nzrbits.hush.feature.notifications.data

import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.CapturedNotification
import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.database.CapturedNotificationDao
import com.nzrbits.hush.core.database.NotificationRuleDao
import com.nzrbits.hush.core.database.toEntity
import com.nzrbits.hush.core.database.toModel
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.feature.notifications.domain.NotificationFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val ruleDao: NotificationRuleDao,
    private val logDao: CapturedNotificationDao,
    private val settings: SettingsRepository,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    val rules: Flow<List<NotificationRule>> = ruleDao.observeAll().map { it.map { e -> e.toModel() } }
    val log: Flow<List<CapturedNotification>> = logDao.observeRecent().map { it.map { e -> e.toModel() } }
    val logCount: Flow<Int> = logDao.observeCount()

    suspend fun rule(id: Long): NotificationRule? = withContext(dispatchers.io) { ruleDao.get(id)?.toModel() }
    suspend fun save(rule: NotificationRule): Long = withContext(dispatchers.io) {
        val rowId = ruleDao.upsert(rule.toEntity())
        if (rule.id != 0L) rule.id else rowId
    }
    suspend fun delete(id: Long) = withContext(dispatchers.io) { ruleDao.delete(id) }
    suspend fun setEnabled(id: Long, enabled: Boolean) = withContext(dispatchers.io) {
        ruleDao.get(id)?.let { ruleDao.upsert(it.copy(enabled = enabled)) }
    }

    /** Decides for an incoming notification. Null means: leave it alone. */
    suspend fun evaluate(packageName: String): NotificationRule? = withContext(dispatchers.io) {
        if (!settings.current().wellbeing.notificationFilterEnabled) return@withContext null
        val rules = ruleDao.enabled().map { it.toModel() }
        NotificationFilter.matchingRule(packageName, clock.nowLocal(), rules)
    }

    suspend fun capture(n: CapturedNotification) = withContext(dispatchers.io) {
        logDao.insert(n.toEntity())
        // Keep the log small: 30 days.
        logDao.purgeOlderThan(clock.now().toEpochMilli() - 30L * 24 * 60 * 60 * 1000)
    }

    suspend fun deleteEntry(id: Long) = withContext(dispatchers.io) { logDao.deleteById(id) }
    suspend fun clearLog() = withContext(dispatchers.io) { logDao.deleteAll() }
    suspend fun isFilterEnabled(): Boolean = settings.settings.first().wellbeing.notificationFilterEnabled
}
