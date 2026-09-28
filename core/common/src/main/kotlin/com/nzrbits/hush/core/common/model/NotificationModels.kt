package com.nzrbits.hush.core.common.model

import java.time.LocalTime

enum class NotificationRuleMode {
    /** Only listed apps are filtered (captured and removed from the shade). */
    BLOCKLIST,

    /** Every app except the listed ones is filtered. */
    ALLOWLIST,
}

/**
 * A filter profile. When [enabled] and the current local time is inside the window
 * (or no window is set), notifications from matching apps are captured into the local log
 * and, where Android permits, removed from the notification shade.
 */
data class NotificationRule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val mode: NotificationRuleMode,
    val packageNames: Set<String>,
    val windowStart: LocalTime? = null,
    val windowEnd: LocalTime? = null,
    val days: Set<java.time.DayOfWeek> = java.time.DayOfWeek.entries.toSet(),
)

/** A notification that a rule captured. Stored locally only. */
data class CapturedNotification(
    val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAtMillis: Long,
    val ruleId: Long?,
)
