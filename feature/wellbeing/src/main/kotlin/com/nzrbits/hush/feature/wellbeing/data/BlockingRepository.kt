package com.nzrbits.hush.feature.wellbeing.data

import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.AppBlock
import com.nzrbits.hush.core.common.model.BlockReason
import com.nzrbits.hush.core.common.model.BlockSchedule
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.database.AppBlockDao
import com.nzrbits.hush.core.database.BlockScheduleDao
import com.nzrbits.hush.core.database.toEntity
import com.nzrbits.hush.core.database.toModel
import com.nzrbits.hush.feature.wellbeing.domain.BlockingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Something the accessibility service just refused. The home screen shows it as an overlay. */
data class BlockedEvent(val packageName: String, val reason: BlockReason, val atMillis: Long)

data class BlockStatus(
    val manual: List<AppBlock>,
    val scheduled: List<Pair<BlockSchedule, Long>>,
) {
    val anyActive: Boolean get() = manual.isNotEmpty() || scheduled.isNotEmpty()
}

@Singleton
class BlockingRepository @Inject constructor(
    private val blockDao: AppBlockDao,
    private val scheduleDao: BlockScheduleDao,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    private val _lastBlocked = MutableStateFlow<BlockedEvent?>(null)
    val lastBlocked: StateFlow<BlockedEvent?> = _lastBlocked

    fun reportBlocked(packageName: String, reason: BlockReason) {
        _lastBlocked.value = BlockedEvent(packageName, reason, clock.now().toEpochMilli())
    }

    fun clearBlockedEvent() { _lastBlocked.value = null }

    /** Emits every minute so time-based state refreshes without a service. */
    private val minuteTicker: Flow<Long> = flow {
        while (true) {
            emit(clock.now().toEpochMilli())
            val ms = clock.now().toEpochMilli()
            delay(60_000L - (ms % 60_000L))
        }
    }

    val activeBlocks: Flow<List<AppBlock>> = minuteTicker.combine(blockDao.observeActive(0)) { now, list ->
        list.map { it.toModel() }.filter { it.isActiveAt(now) }
    }

    val schedules: Flow<List<BlockSchedule>> = scheduleDao.observeAll().map { it.map { e -> e.toModel() } }

    val status: Flow<BlockStatus> = combine(minuteTicker, activeBlocks, schedules) { _, blocks, schedules ->
        BlockStatus(blocks, BlockingEngine.activeSchedules(clock.nowLocal(), schedules))
    }

    suspend fun reasonFor(packageName: String): BlockReason? = withContext(dispatchers.io) {
        val now = clock.nowLocal()
        val nowMillis = now.toInstant().toEpochMilli()
        val blocks = blockDao.activeAt(nowMillis).map { it.toModel() }
        val schedules = scheduleDao.enabled().map { it.toModel() }
        BlockingEngine.reasonFor(packageName, now, blocks, schedules)
    }

    suspend fun block(packageName: String, minutes: Long, note: String? = null): AppBlock = withContext(dispatchers.io) {
        val clamped = minutes.coerceIn(HushConfig.MIN_BLOCK_MINUTES, HushConfig.MAX_BLOCK_MINUTES)
        val start = clock.now().toEpochMilli()
        val block = AppBlock(packageName = packageName, startedAtMillis = start, endsAtMillis = start + clamped * 60_000L, note = note)
        val id = blockDao.insert(block.toEntity())
        block.copy(id = id)
    }

    suspend fun unblock(id: Long) = withContext(dispatchers.io) { blockDao.delete(id) }

    suspend fun purgeExpired() = withContext(dispatchers.io) {
        blockDao.purgeExpired(clock.now().toEpochMilli() - 7L * 24 * 60 * 60 * 1000)
    }

    suspend fun schedule(id: Long): BlockSchedule? = withContext(dispatchers.io) { scheduleDao.get(id)?.toModel() }

    suspend fun saveSchedule(schedule: BlockSchedule): Long = withContext(dispatchers.io) {
        val rowId = scheduleDao.upsert(schedule.toEntity())
        if (schedule.id != 0L) schedule.id else rowId
    }

    suspend fun deleteSchedule(id: Long) = withContext(dispatchers.io) { scheduleDao.delete(id) }

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) = withContext(dispatchers.io) {
        val existing = scheduleDao.get(id) ?: return@withContext
        scheduleDao.upsert(existing.copy(enabled = enabled))
    }

    suspend fun currentStatus(): BlockStatus = status.first()
}
