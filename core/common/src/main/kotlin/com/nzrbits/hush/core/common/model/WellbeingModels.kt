package com.nzrbits.hush.core.common.model

import java.time.DayOfWeek
import java.time.LocalTime

/** A manual, one-off block of a single app. Times are epoch milliseconds. */
data class AppBlock(
    val id: Long = 0,
    val packageName: String,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val note: String? = null,
) {
    fun isActiveAt(nowMillis: Long): Boolean = nowMillis in startedAtMillis until endsAtMillis
}

/**
 * A recurring block. [startTime] and [endTime] are local wall-clock times. If [endTime] is
 * before or equal to [startTime], the schedule crosses midnight and ends on the next day.
 */
data class BlockSchedule(
    val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val days: Set<DayOfWeek>,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val packageNames: Set<String>,
) {
    val crossesMidnight: Boolean get() = !endTime.isAfter(startTime)
}

/** A soft daily usage limit. This is a reminder, not a block. */
data class UsageLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,
    val warnAtPercent: Int = 80,
)

/** Reason a launch is refused. Used for the blocked screen and for logs. */
sealed interface BlockReason {
    data class Manual(val block: AppBlock) : BlockReason
    data class Scheduled(val schedule: BlockSchedule, val endsAtMillis: Long) : BlockReason
}

enum class ShortVideoPlatform(val packageName: String, val displayName: String) {
    YOUTUBE_SHORTS("com.google.android.youtube", "YouTube Shorts"),
    INSTAGRAM_REELS("com.instagram.android", "Instagram Reels"),
    FACEBOOK_REELS("com.facebook.katana", "Facebook Reels"),
    SNAPCHAT_SPOTLIGHT("com.snapchat.android", "Snapchat Spotlight"),
}

/** Aggregated foreground time for one package. */
data class AppUsage(
    val packageName: String,
    val foregroundMillis: Long,
    val launchCount: Int,
)

data class DailyUsage(
    val dayStartMillis: Long,
    val totalMillis: Long,
    val perApp: List<AppUsage>,
)
