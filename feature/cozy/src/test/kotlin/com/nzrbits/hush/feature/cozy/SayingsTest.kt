package com.nzrbits.hush.feature.cozy

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

class SayingsTest {
    @Test
    fun daypartBoundariesMatchWebApp() {
        assertThat(Sayings.daypart(4)).isEqualTo(Daypart.NIGHT)
        assertThat(Sayings.daypart(5)).isEqualTo(Daypart.MORNING)
        assertThat(Sayings.daypart(11)).isEqualTo(Daypart.DAY)
        assertThat(Sayings.daypart(17)).isEqualTo(Daypart.EVENING)
        assertThat(Sayings.daypart(23)).isEqualTo(Daypart.NIGHT)
    }

    @Test
    fun stableWithinTheHour() {
        val a = Sayings.home(LocalDateTime.of(2026, 9, 28, 20, 5))
        val b = Sayings.home(LocalDateTime.of(2026, 9, 28, 20, 55))
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun everyContextReturnsText() {
        val now = LocalDateTime.of(2026, 9, 28, 9, 0)
        assertThat(Sayings.home(now)).isNotEmpty()
        assertThat(Sayings.settings(now)).isNotEmpty()
        assertThat(Sayings.blocked(now)).isNotEmpty()
        assertThat(Sayings.focus(now)).isNotEmpty()
        assertThat(Sayings.limitReached(now)).isNotEmpty()
    }
}
