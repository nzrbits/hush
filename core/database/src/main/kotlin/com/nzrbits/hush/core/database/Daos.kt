package com.nzrbits.hush.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCustomizationDao {
    @Query("SELECT * FROM app_customization")
    fun observeAll(): Flow<List<AppCustomizationEntity>>

    @Query("SELECT * FROM app_customization WHERE appId = :appId")
    suspend fun get(appId: String): AppCustomizationEntity?

    @Upsert
    suspend fun upsert(entity: AppCustomizationEntity)

    @Query("SELECT COALESCE(MAX(favoriteOrder), -1) FROM app_customization")
    suspend fun maxFavoriteOrder(): Int

    @Query("UPDATE app_customization SET favoriteOrder = :order WHERE appId = :appId")
    suspend fun setFavoriteOrder(appId: String, order: Int?)

    @Query("UPDATE app_customization SET folderId = NULL WHERE folderId = :folderId")
    suspend fun clearFolder(folderId: Long)

    @Query("DELETE FROM app_customization WHERE packageName = :packageName")
    suspend fun deleteForPackage(packageName: String)

    @Transaction
    suspend fun reorderFavorites(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> setFavoriteOrder(id, index) }
    }
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface AppBlockDao {
    @Query("SELECT * FROM app_blocks WHERE endsAtMillis > :nowMillis ORDER BY endsAtMillis")
    fun observeActive(nowMillis: Long): Flow<List<AppBlockEntity>>

    @Query("SELECT * FROM app_blocks WHERE endsAtMillis > :nowMillis")
    suspend fun activeAt(nowMillis: Long): List<AppBlockEntity>

    @Query("SELECT * FROM app_blocks WHERE packageName = :packageName AND endsAtMillis > :nowMillis ORDER BY endsAtMillis DESC LIMIT 1")
    suspend fun activeForPackage(packageName: String, nowMillis: Long): AppBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: AppBlockEntity): Long

    @Query("DELETE FROM app_blocks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM app_blocks WHERE endsAtMillis < :beforeMillis")
    suspend fun purgeExpired(beforeMillis: Long)
}

@Dao
interface BlockScheduleDao {
    @Query("SELECT * FROM block_schedules ORDER BY name")
    fun observeAll(): Flow<List<BlockScheduleEntity>>

    @Query("SELECT * FROM block_schedules WHERE enabled = 1")
    suspend fun enabled(): List<BlockScheduleEntity>

    @Query("SELECT * FROM block_schedules WHERE id = :id")
    suspend fun get(id: Long): BlockScheduleEntity?

    @Upsert
    suspend fun upsert(schedule: BlockScheduleEntity): Long

    @Query("DELETE FROM block_schedules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface UsageLimitDao {
    @Query("SELECT * FROM usage_limits ORDER BY packageName")
    fun observeAll(): Flow<List<UsageLimitEntity>>

    @Query("SELECT * FROM usage_limits")
    suspend fun all(): List<UsageLimitEntity>

    @Query("SELECT * FROM usage_limits WHERE packageName = :packageName")
    suspend fun get(packageName: String): UsageLimitEntity?

    @Upsert
    suspend fun upsert(limit: UsageLimitEntity)

    @Query("DELETE FROM usage_limits WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Query("UPDATE usage_limits SET lastReminderEpochDay = :day WHERE packageName = :packageName")
    suspend fun markReminded(packageName: String, day: Long)

    @Query("UPDATE usage_limits SET lastWarningEpochDay = :day WHERE packageName = :packageName")
    suspend fun markWarned(packageName: String, day: Long)
}

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules ORDER BY name")
    fun observeAll(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT * FROM notification_rules WHERE enabled = 1")
    suspend fun enabled(): List<NotificationRuleEntity>

    @Query("SELECT * FROM notification_rules WHERE id = :id")
    suspend fun get(id: Long): NotificationRuleEntity?

    @Upsert
    suspend fun upsert(rule: NotificationRuleEntity): Long

    @Query("DELETE FROM notification_rules WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CapturedNotificationDao {
    @Query("SELECT * FROM captured_notifications ORDER BY postedAtMillis DESC LIMIT 500")
    fun observeRecent(): Flow<List<CapturedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM captured_notifications")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: CapturedNotificationEntity): Long

    @Delete
    suspend fun delete(entity: CapturedNotificationEntity)

    @Query("DELETE FROM captured_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captured_notifications")
    suspend fun deleteAll()

    @Query("DELETE FROM captured_notifications WHERE postedAtMillis < :beforeMillis")
    suspend fun purgeOlderThan(beforeMillis: Long)
}
