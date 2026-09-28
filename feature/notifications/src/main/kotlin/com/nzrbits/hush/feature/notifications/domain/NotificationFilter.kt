package com.nzrbits.hush.feature.notifications.domain

import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.model.NotificationRuleMode
import java.time.ZonedDateTime

/** Pure rule evaluation. Returns the first rule that captures the notification, or null. */
object NotificationFilter {
    fun matchingRule(packageName: String, now: ZonedDateTime, rules: List<NotificationRule>): NotificationRule? =
        rules.firstOrNull { rule -> rule.enabled && inWindow(rule, now) && appliesTo(rule, packageName) }

    fun appliesTo(rule: NotificationRule, packageName: String): Boolean = when (rule.mode) {
        NotificationRuleMode.BLOCKLIST -> packageName in rule.packageNames
        NotificationRuleMode.ALLOWLIST -> packageName !in rule.packageNames
    }

    /** No window means always. A window whose end is not after its start crosses midnight. */
    fun inWindow(rule: NotificationRule, now: ZonedDateTime): Boolean {
        val start = rule.windowStart
        val end = rule.windowEnd
        val today = now.dayOfWeek
        val time = now.toLocalTime()
        if (start == null || end == null) return today in rule.days
        return if (end.isAfter(start)) {
            today in rule.days && !time.isBefore(start) && time.isBefore(end)
        } else {
            (today in rule.days && !time.isBefore(start)) ||
                (now.minusDays(1).dayOfWeek in rule.days && time.isBefore(end))
        }
    }
}
