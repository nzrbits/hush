package com.nzrbits.hush.core.database

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.BlockSchedule
import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.model.NotificationRuleMode
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class MappersTest {
    @Test
    fun daysMaskRoundTrip() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY)
        assertThat(DaysMask.decode(DaysMask.encode(days))).isEqualTo(days)
        assertThat(DaysMask.encode(emptySet())).isEqualTo(0)
        assertThat(DaysMask.decode(0b1111111)).hasSize(7)
    }

    @Test
    fun packagesCsvRoundTripIgnoresBlanks() {
        val packages = setOf("com.b", "com.a")
        val csv = PackagesCsv.encode(packages)
        assertThat(csv).isEqualTo("com.a,com.b")
        assertThat(PackagesCsv.decode("com.a, ,com.b,")).isEqualTo(packages)
        assertThat(PackagesCsv.decode("")).isEmpty()
    }

    @Test
    fun scheduleRoundTrip() {
        val s = BlockSchedule(5, "Focus", true, setOf(DayOfWeek.FRIDAY), LocalTime.of(22, 15), LocalTime.of(2, 0), setOf("com.x"))
        assertThat(s.toEntity().toModel()).isEqualTo(s)
    }

    @Test
    fun notificationRuleRoundTripWithAndWithoutWindow() {
        val withWindow = NotificationRule(3, "Nacht", true, NotificationRuleMode.ALLOWLIST, setOf("com.dialer"), LocalTime.of(22, 0), LocalTime.of(7, 0))
        assertThat(withWindow.toEntity().toModel()).isEqualTo(withWindow)
        val noWindow = withWindow.copy(windowStart = null, windowEnd = null)
        assertThat(noWindow.toEntity().toModel()).isEqualTo(noWindow)
    }

    @Test
    fun unknownModeFallsBackToBlocklist() {
        val entity = NotificationRuleEntity(1, "x", true, "GARBAGE", "", null, null, 127)
        assertThat(entity.toModel().mode).isEqualTo(NotificationRuleMode.BLOCKLIST)
    }
}
