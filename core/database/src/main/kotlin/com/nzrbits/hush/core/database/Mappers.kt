package com.nzrbits.hush.core.database

import com.nzrbits.hush.core.common.model.AppBlock
import com.nzrbits.hush.core.common.model.AppCustomization
import com.nzrbits.hush.core.common.model.AppFolder
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.BlockSchedule
import com.nzrbits.hush.core.common.model.CapturedNotification
import com.nzrbits.hush.core.common.model.NotificationRule
import com.nzrbits.hush.core.common.model.NotificationRuleMode
import com.nzrbits.hush.core.common.model.UsageLimit
import java.time.DayOfWeek
import java.time.LocalTime

/** Bitmask helpers: Monday = bit 0 ... Sunday = bit 6. */
object DaysMask {
    fun encode(days: Set<DayOfWeek>): Int = days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
    fun decode(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}

object PackagesCsv {
    fun encode(packages: Set<String>): String = packages.filter { it.isNotBlank() }.sorted().joinToString(",")
    fun decode(csv: String): Set<String> = csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

fun AppCustomizationEntity.toModel() = AppCustomization(
    key = AppKey(packageName, userSerial),
    customLabel = customLabel,
    hidden = hidden,
    favoriteOrder = favoriteOrder,
    folderId = folderId,
)

fun AppCustomization.toEntity() = AppCustomizationEntity(
    appId = key.id,
    packageName = key.packageName,
    userSerial = key.userSerial,
    customLabel = customLabel,
    hidden = hidden,
    favoriteOrder = favoriteOrder,
    folderId = folderId,
)

fun FolderEntity.toModel() = AppFolder(id, name, sortOrder)

fun AppBlockEntity.toModel() = AppBlock(id, packageName, startedAtMillis, endsAtMillis, note)
fun AppBlock.toEntity() = AppBlockEntity(id, packageName, startedAtMillis, endsAtMillis, note)

fun BlockScheduleEntity.toModel() = BlockSchedule(
    id = id,
    name = name,
    enabled = enabled,
    days = DaysMask.decode(daysMask),
    startTime = LocalTime.ofSecondOfDay(startMinutes * 60L),
    endTime = LocalTime.ofSecondOfDay(endMinutes * 60L),
    packageNames = PackagesCsv.decode(packages),
)

fun BlockSchedule.toEntity() = BlockScheduleEntity(
    id = id,
    name = name,
    enabled = enabled,
    daysMask = DaysMask.encode(days),
    startMinutes = startTime.toSecondOfDay() / 60,
    endMinutes = endTime.toSecondOfDay() / 60,
    packages = PackagesCsv.encode(packageNames),
)

fun UsageLimitEntity.toModel() = UsageLimit(packageName, dailyLimitMinutes, warnAtPercent)

fun NotificationRuleEntity.toModel() = NotificationRule(
    id = id,
    name = name,
    enabled = enabled,
    mode = runCatching { NotificationRuleMode.valueOf(mode) }.getOrDefault(NotificationRuleMode.BLOCKLIST),
    packageNames = PackagesCsv.decode(packages),
    windowStart = windowStartMinutes?.let { LocalTime.ofSecondOfDay(it * 60L) },
    windowEnd = windowEndMinutes?.let { LocalTime.ofSecondOfDay(it * 60L) },
    days = DaysMask.decode(daysMask),
)

fun NotificationRule.toEntity() = NotificationRuleEntity(
    id = id,
    name = name,
    enabled = enabled,
    mode = mode.name,
    packages = PackagesCsv.encode(packageNames),
    windowStartMinutes = windowStart?.let { it.toSecondOfDay() / 60 },
    windowEndMinutes = windowEnd?.let { it.toSecondOfDay() / 60 },
    daysMask = DaysMask.encode(days),
)

fun CapturedNotificationEntity.toModel() = CapturedNotification(id, packageName, appLabel, title, text, postedAtMillis, ruleId)
fun CapturedNotification.toEntity() = CapturedNotificationEntity(id, packageName, appLabel, title, text, postedAtMillis, ruleId)
