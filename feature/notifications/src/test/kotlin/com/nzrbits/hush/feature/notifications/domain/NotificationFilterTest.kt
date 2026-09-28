package com.nzrbits.hush.feature.notifications.domain

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.model.NotificationRuleMode
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationFilterTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 9, day, hour, minute, 0, 0, zone) // 28 = Monday

    private val night = NotificationRule(
        id = 1, name = "Nacht", enabled = true, mode = NotificationRuleMode.ALLOWLIST,
        packageNames = setOf("com.android.dialer"),
        windowStart = LocalTime.of(22, 0), windowEnd = LocalTime.of(7, 0),
    )
    private val social = NotificationRule(
        id = 2, name = "Social", enabled = true, mode = NotificationRuleMode.BLOCKLIST,
        packageNames = setOf("com.instagram.android"),
    )

    @Test
    fun blocklistCapturesListedOnly() {
        assertThat(NotificationFilter.matchingRule("com.instagram.android", at(28, 12), listOf(social))?.id).isEqualTo(2)
        assertThat(NotificationFilter.matchingRule("com.whatsapp", at(28, 12), listOf(social))).isNull()
    }

    @Test
    fun allowlistCapturesEverythingElse() {
        assertThat(NotificationFilter.matchingRule("com.whatsapp", at(28, 23), listOf(night))?.id).isEqualTo(1)
        assertThat(NotificationFilter.matchingRule("com.android.dialer", at(28, 23), listOf(night))).isNull()
    }

    @Test
    fun overnightWindowCoversMorningOfNextDay() {
        assertThat(NotificationFilter.inWindow(night, at(29, 6, 30))).isTrue()
        assertThat(NotificationFilter.inWindow(night, at(29, 7, 0))).isFalse()
        assertThat(NotificationFilter.inWindow(night, at(29, 12, 0))).isFalse()
    }

    @Test
    fun disabledRuleNeverMatches() {
        assertThat(NotificationFilter.matchingRule("com.instagram.android", at(28, 12), listOf(social.copy(enabled = false)))).isNull()
    }

    @Test
    fun dayRestrictionRespected() {
        val weekend = social.copy(days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertThat(NotificationFilter.matchingRule("com.instagram.android", at(28, 12), listOf(weekend))).isNull()
        assertThat(NotificationFilter.matchingRule("com.instagram.android", ZonedDateTime.of(2026, 10, 3, 12, 0, 0, 0, zone), listOf(weekend))).isNotNull()
    }

    @Test
    fun firstMatchingRuleWins() {
        val rules = listOf(night, social)
        assertThat(NotificationFilter.matchingRule("com.instagram.android", at(28, 23), rules)?.id).isEqualTo(1)
        assertThat(NotificationFilter.matchingRule("com.instagram.android", at(28, 12), rules)?.id).isEqualTo(2)
    }
}
