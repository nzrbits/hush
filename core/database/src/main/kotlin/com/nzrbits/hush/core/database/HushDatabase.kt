package com.nzrbits.hush.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nzrbits.hush.core.common.HushConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        AppCustomizationEntity::class,
        FolderEntity::class,
        AppBlockEntity::class,
        BlockScheduleEntity::class,
        UsageLimitEntity::class,
        NotificationRuleEntity::class,
        CapturedNotificationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HushDatabase : RoomDatabase() {
    abstract fun appCustomizationDao(): AppCustomizationDao
    abstract fun folderDao(): FolderDao
    abstract fun appBlockDao(): AppBlockDao
    abstract fun blockScheduleDao(): BlockScheduleDao
    abstract fun usageLimitDao(): UsageLimitDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun capturedNotificationDao(): CapturedNotificationDao

    companion object {
        const val NAME = "${HushConfig.STORAGE_NAMESPACE}.db"

        fun build(context: Context): HushDatabase =
            Room.databaseBuilder(context, HushDatabase::class.java, NAME)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HushDatabase = HushDatabase.build(context)

    @Provides fun appCustomizationDao(db: HushDatabase) = db.appCustomizationDao()
    @Provides fun folderDao(db: HushDatabase) = db.folderDao()
    @Provides fun appBlockDao(db: HushDatabase) = db.appBlockDao()
    @Provides fun blockScheduleDao(db: HushDatabase) = db.blockScheduleDao()
    @Provides fun usageLimitDao(db: HushDatabase) = db.usageLimitDao()
    @Provides fun notificationRuleDao(db: HushDatabase) = db.notificationRuleDao()
    @Provides fun capturedNotificationDao(db: HushDatabase) = db.capturedNotificationDao()
}
