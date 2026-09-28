package com.nzrbits.hush.feature.wellbeing.data

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.UsageLimit
import org.junit.Test

class LimitDecisionTest {
    private val limit = UsageLimit("com.instagram.android", dailyLimitMinutes = 30, warnAtPercent = 80)

    @Test
    fun belowWarningDoesNothing() {
        assertThat(LimitDecision.decide(10, limit, remindedToday = false, warnedToday = false)).isEqualTo(LimitDecision.Outcome.NONE)
    }

    @Test
    fun warningAtEightyPercentOnce() {
        assertThat(LimitDecision.decide(24, limit, false, false)).isEqualTo(LimitDecision.Outcome.WARN)
        assertThat(LimitDecision.decide(26, limit, false, warnedToday = true)).isEqualTo(LimitDecision.Outcome.NONE)
    }

    @Test
    fun reminderAtLimitOnce() {
        assertThat(LimitDecision.decide(30, limit, false, false)).isEqualTo(LimitDecision.Outcome.REMIND)
        assertThat(LimitDecision.decide(45, limit, remindedToday = true, warnedToday = true)).isEqualTo(LimitDecision.Outcome.NONE)
    }

    @Test
    fun reminderDoesNotDependOnWarning() {
        assertThat(LimitDecision.decide(31, limit, remindedToday = false, warnedToday = false)).isEqualTo(LimitDecision.Outcome.REMIND)
    }
}
