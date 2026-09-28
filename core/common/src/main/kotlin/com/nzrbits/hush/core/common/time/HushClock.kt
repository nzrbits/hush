package com.nzrbits.hush.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Time source used by all domain logic so that tests can freeze time and the app stays
 * consistent when the user changes the system clock.
 */
interface HushClock {
    fun now(): Instant
    fun zone(): ZoneId

    fun nowLocal(): ZonedDateTime = now().atZone(zone())
    fun today(): LocalDate = nowLocal().toLocalDate()
    fun localTime(): LocalTime = nowLocal().toLocalTime()

    /** Start of the given local day as an epoch millisecond, DST safe. */
    fun startOfDayMillis(date: LocalDate = today()): Long =
        date.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun toLocal(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone()).toLocalDateTime()
}

class SystemClock : HushClock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/** Fixed clock for tests. */
class FixedClock(private var instant: Instant, private val zoneId: ZoneId = ZoneId.of("Europe/Berlin")) : HushClock {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
    fun advance(millis: Long) { instant = instant.plusMillis(millis) }
    fun set(newInstant: Instant) { instant = newInstant }
}
