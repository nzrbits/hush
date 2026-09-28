package com.nzrbits.hush.core.common

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.text.Search
import com.nzrbits.hush.core.common.time.Durations
import com.nzrbits.hush.core.common.time.FixedClock
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SearchTest {
    @Test
    fun normalizeStripsAccentsAndCase() {
        assertThat(Search.normalize("Über Café")).isEqualTo("uber cafe")
    }

    @Test
    fun rankPrefersPrefixThenWordStartThenSubstring() {
        assertThat(Search.rank("whatsapp", "what")).isEqualTo(0)
        assertThat(Search.rank("google maps", "map")).isEqualTo(1)
        assertThat(Search.rank("chromecast", "rome")).isEqualTo(2)
        assertThat(Search.rank("spotify", "xyz")).isNull()
    }

    @Test
    fun emptyQueryMatchesEverything() {
        assertThat(Search.rank("anything", "")).isEqualTo(0)
    }

    @Test
    fun indexLetterMapsDigitsToHash() {
        assertThat(Search.indexLetter("1Password")).isEqualTo('#')
        assertThat(Search.indexLetter("Ärzte")).isEqualTo('A')
        assertThat(Search.indexLetter("zoom")).isEqualTo('Z')
    }
}

class AppKeyTest {
    @Test
    fun roundTrip() {
        val key = AppKey("com.example.app", 10)
        assertThat(AppKey.parse(key.id)).isEqualTo(key)
    }

    @Test
    fun legacyIdWithoutUserDefaultsToPersonal() {
        assertThat(AppKey.parse("com.example.app")).isEqualTo(AppKey("com.example.app", AppKey.PERSONAL_USER))
    }
}

class DurationsTest {
    @Test
    fun formatsMinutesHoursDays() {
        assertThat(Durations.formatMinutes(45)).isEqualTo("45 Min.")
        assertThat(Durations.formatMinutes(60)).isEqualTo("1 Std.")
        assertThat(Durations.formatMinutes(90)).isEqualTo("1 Std. 30 Min.")
        assertThat(Durations.formatMinutes(24 * 60)).isEqualTo("1 Tag")
        assertThat(Durations.formatMinutes(30 * 24 * 60)).isEqualTo("30 Tage")
        assertThat(Durations.formatMinutes(25 * 60)).isEqualTo("1 Tag 1 Std.")
    }

    @Test
    fun formatsMillisShort() {
        assertThat(Durations.formatMillisShort(30_000)).isEqualTo("<1 Min.")
        assertThat(Durations.formatMillisShort(5 * 60_000)).isEqualTo("5 Min.")
        assertThat(Durations.formatMillisShort(125 * 60_000)).isEqualTo("2 Std. 5 Min.")
    }
}

class ClockTest {
    @Test
    fun startOfDayIsDstSafe() {
        // 29 March 2026: clocks go forward in Europe/Berlin.
        val clock = FixedClock(Instant.parse("2026-03-29T12:00:00Z"))
        val start = clock.startOfDayMillis(LocalDate.of(2026, 3, 29))
        val next = clock.startOfDayMillis(LocalDate.of(2026, 3, 30))
        assertThat(next - start).isEqualTo(23L * 60 * 60 * 1000)
    }
}
