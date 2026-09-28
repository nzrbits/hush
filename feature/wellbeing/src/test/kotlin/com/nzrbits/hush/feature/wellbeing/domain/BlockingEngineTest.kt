package com.nzrbits.hush.feature.wellbeing.domain

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.AppBlock
import com.nzrbits.hush.core.common.model.BlockReason
import com.nzrbits.hush.core.common.model.BlockSchedule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BlockingEngineTest {
    private val zone: ZoneId = ZoneId.of("Europe/Berlin")

    // 2026-09-28 is a Monday.
    private fun at(day: Int, hour: Int, minute: Int = 0, month: Int = 9): ZonedDateTime =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)

    private val focus = BlockSchedule(
        id = 1, name = "Focus", enabled = true,
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        startTime = LocalTime.of(9, 0), endTime = LocalTime.of(12, 0),
        packageNames = setOf("com.instagram.android", "com.google.android.youtube"),
    )

    @Test
    fun manualBlockActiveInsideWindow() {
        val now = at(28, 10)
        val block = AppBlock(1, "com.x", now.toInstant().toEpochMilli() - 1000, now.toInstant().toEpochMilli() + 60_000)
        val reason = BlockingEngine.reasonFor("com.x", now, listOf(block), emptyList())
        assertThat(reason).isInstanceOf(BlockReason.Manual::class.java)
    }

    @Test
    fun manualBlockExpiredIsNotActive() {
        val now = at(28, 10)
        val block = AppBlock(1, "com.x", now.toInstant().toEpochMilli() - 120_000, now.toInstant().toEpochMilli() - 1)
        assertThat(BlockingEngine.reasonFor("com.x", now, listOf(block), emptyList())).isNull()
    }

    @Test
    fun manualBlockIsPerPackage() {
        val now = at(28, 10)
        val block = AppBlock(1, "com.x", 0, now.toInstant().toEpochMilli() + 60_000)
        assertThat(BlockingEngine.reasonFor("com.y", now, listOf(block), emptyList())).isNull()
    }

    @Test
    fun scheduleActiveOnWeekdayInsideWindow() {
        val reason = BlockingEngine.reasonFor("com.instagram.android", at(28, 10, 30), emptyList(), listOf(focus))
        assertThat(reason).isInstanceOf(BlockReason.Scheduled::class.java)
        val end = (reason as BlockReason.Scheduled).endsAtMillis
        assertThat(end).isEqualTo(at(28, 12).toInstant().toEpochMilli())
    }

    @Test
    fun scheduleNotActiveOutsideWindowOrOnWeekend() {
        assertThat(BlockingEngine.reasonFor("com.instagram.android", at(28, 12, 0), emptyList(), listOf(focus))).isNull()
        assertThat(BlockingEngine.reasonFor("com.instagram.android", at(28, 8, 59), emptyList(), listOf(focus))).isNull()
        // Saturday 3 Oct 2026
        assertThat(BlockingEngine.reasonFor("com.instagram.android", at(3, 10, 0, month = 10), emptyList(), listOf(focus))).isNull()
    }

    @Test
    fun disabledScheduleIsIgnored() {
        assertThat(BlockingEngine.reasonFor("com.instagram.android", at(28, 10), emptyList(), listOf(focus.copy(enabled = false)))).isNull()
    }

    @Test
    fun scheduleOnlyAppliesToListedPackages() {
        assertThat(BlockingEngine.reasonFor("com.whatsapp", at(28, 10), emptyList(), listOf(focus))).isNull()
    }

    @Test
    fun overnightScheduleCoversEveningAndNextMorning() {
        val night = focus.copy(days = setOf(DayOfWeek.FRIDAY), startTime = LocalTime.of(22, 0), endTime = LocalTime.of(2, 0))
        // Friday 2 Oct 2026, 23:00 -> active, ends Saturday 02:00
        val evening = ScheduleEvaluator.activeUntil(night, at(2, 23, 0, month = 10))
        assertThat(evening).isEqualTo(at(3, 2, 0, month = 10).toInstant().toEpochMilli())
        // Saturday 01:00 -> still active (window started Friday)
        val morning = ScheduleEvaluator.activeUntil(night, at(3, 1, 0, month = 10))
        assertThat(morning).isEqualTo(at(3, 2, 0, month = 10).toInstant().toEpochMilli())
        // Saturday 02:00 -> over
        assertThat(ScheduleEvaluator.activeUntil(night, at(3, 2, 0, month = 10))).isNull()
        // Thursday 23:00 -> not a scheduled day
        assertThat(ScheduleEvaluator.activeUntil(night, at(1, 23, 0, month = 10))).isNull()
    }

    @Test
    fun manualBlockWinsOverSchedule() {
        val now = at(28, 10)
        val block = AppBlock(7, "com.instagram.android", 0, now.toInstant().toEpochMilli() + 1)
        val reason = BlockingEngine.reasonFor("com.instagram.android", now, listOf(block), listOf(focus))
        assertThat(reason).isInstanceOf(BlockReason.Manual::class.java)
    }

    @Test
    fun dstTransitionKeepsWallClockWindow() {
        // Clocks go back on Sunday 25 Oct 2026 in Europe/Berlin. A Sunday schedule 01:00-04:00 still ends at wall-clock 04:00.
        val sunday = focus.copy(days = setOf(DayOfWeek.SUNDAY), startTime = LocalTime.of(1, 0), endTime = LocalTime.of(4, 0))
        val now = ZonedDateTime.of(2026, 10, 25, 1, 30, 0, 0, zone)
        val end = ScheduleEvaluator.activeUntil(sunday, now)
        val endLocal = java.time.Instant.ofEpochMilli(end!!).atZone(zone)
        assertThat(endLocal.hour).isEqualTo(4)
        assertThat(endLocal.dayOfMonth).isEqualTo(25)
    }
}
