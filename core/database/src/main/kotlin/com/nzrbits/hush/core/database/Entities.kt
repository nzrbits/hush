package com.nzrbits.hush.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Per-app customisation. Key is `package@userSerial`, see AppKey.id. */
@Entity(tableName = "app_customization")
data class AppCustomizationEntity(
    @PrimaryKey val appId: String,
    val packageName: String,
    val userSerial: Long,
    val customLabel: String?,
    val hidden: Boolean,
    val favoriteOrder: Int?,
    val folderId: Long?,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
)

@Entity(tableName = "app_blocks", indices = [Index("packageName"), Index("endsAtMillis")])
data class AppBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val note: String?,
)

/** Days are stored as a bitmask, Monday = bit 0. Times are minutes since midnight. Packages are comma separated. */
@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    val daysMask: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val packages: String,
)

@Entity(tableName = "usage_limits")
data class UsageLimitEntity(
    @PrimaryKey val packageName: String,
    val dailyLimitMinutes: Int,
    val warnAtPercent: Int,
    /** Local day (epoch day) on which the last limit reminder was sent, so we remind once per day. */
    val lastReminderEpochDay: Long?,
    val lastWarningEpochDay: Long?,
)

@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enabled: Boolean,
    /** "BLOCKLIST" or "ALLOWLIST". */
    val mode: String,
    val packages: String,
    val windowStartMinutes: Int?,
    val windowEndMinutes: Int?,
    val daysMask: Int,
)

@Entity(tableName = "captured_notifications", indices = [Index("postedAtMillis")])
data class CapturedNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAtMillis: Long,
    val ruleId: Long?,
)
