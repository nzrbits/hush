package com.nzrbits.hush.feature.wellbeing.domain

import com.nzrbits.hush.core.common.model.AppBlock
import com.nzrbits.hush.core.common.model.BlockReason
import com.nzrbits.hush.core.common.model.BlockSchedule
import java.time.ZonedDateTime

/**
 * Pure decision logic: is a package blocked right now, and why. No Android, fully testable.
 */
object BlockingEngine {
    fun reasonFor(
        packageName: String,
        now: ZonedDateTime,
        blocks: List<AppBlock>,
        schedules: List<BlockSchedule>,
    ): BlockReason? {
        val nowMillis = now.toInstant().toEpochMilli()
        blocks.firstOrNull { it.packageName == packageName && it.isActiveAt(nowMillis) }
            ?.let { return BlockReason.Manual(it) }
        for (schedule in schedules) {
            if (!schedule.enabled || packageName !in schedule.packageNames) continue
            val end = ScheduleEvaluator.activeUntil(schedule, now) ?: continue
            return BlockReason.Scheduled(schedule, end)
        }
        return null
    }

    /** All schedules active at [now], with their end time. Used for the home screen mascot state. */
    fun activeSchedules(now: ZonedDateTime, schedules: List<BlockSchedule>): List<Pair<BlockSchedule, Long>> =
        schedules.filter { it.enabled }.mapNotNull { s -> ScheduleEvaluator.activeUntil(s, now)?.let { s to it } }
}

object ScheduleEvaluator {
    /**
     * Returns the epoch millisecond when the schedule's current window ends, or null if it is
     * not active at [now]. A window that crosses midnight counts on the day it *started*: a
     * schedule "Fri 22:00–02:00" is active on Saturday 01:00 because Friday is in its days.
     * Local wall-clock times are used, so DST changes shift the window with the clock.
     */
    fun activeUntil(schedule: BlockSchedule, now: ZonedDateTime): Long? {
        val today = now.toLocalDate()
        val time = now.toLocalTime()
        if (!schedule.crossesMidnight) {
            if (today.dayOfWeek !in schedule.days) return null
            if (time < schedule.startTime || !time.isBefore(schedule.endTime)) return null
            return today.atTime(schedule.endTime).atZone(now.zone).toInstant().toEpochMilli()
        }
        // Crossing midnight: either we are in the evening part (started today) ...
        if (today.dayOfWeek in schedule.days && !time.isBefore(schedule.startTime)) {
            return today.plusDays(1).atTime(schedule.endTime).atZone(now.zone).toInstant().toEpochMilli()
        }
        // ... or in the morning part of a window that started yesterday.
        val yesterday = today.minusDays(1)
        if (yesterday.dayOfWeek in schedule.days && time.isBefore(schedule.endTime)) {
            return today.atTime(schedule.endTime).atZone(now.zone).toInstant().toEpochMilli()
        }
        return null
    }
}
