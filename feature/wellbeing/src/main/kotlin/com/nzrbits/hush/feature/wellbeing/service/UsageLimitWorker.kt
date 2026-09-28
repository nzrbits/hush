package com.nzrbits.hush.feature.wellbeing.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import com.nzrbits.hush.feature.wellbeing.data.UsageLimitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Fallback for usage limit reminders when the accessibility service is off, and housekeeping
 * for expired blocks. Runs every 15 minutes (WorkManager minimum), so a reminder can arrive a
 * few minutes late in that mode. Not a foreground service: nothing runs permanently.
 */
@HiltWorker
class UsageLimitWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val limits: UsageLimitRepository,
    private val blocking: BlockingRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runCatching { limits.checkAll() }
        runCatching { blocking.purgeExpired() }
        return Result.success()
    }

    companion object {
        private const val NAME = "hush.usageLimits"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageLimitWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
