package com.nzrbits.hush.feature.updates

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nzrbits.hush.core.datastore.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Every 6 hours, only with network and only while "automatisch prüfen" is on. With
 * "automatisch installieren" it also downloads and hands the APK to the installer, which
 * on Android 12+ is silent once Hush is its own installer of record.
 */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: UpdateRepository,
    private val settings: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = settings.settings.first().updates
        if (!prefs.autoCheck) return Result.success()
        val release = repository.check(force = true) ?: return Result.success()
        if (prefs.autoInstall && repository.canInstall()) {
            val file = repository.download(release) ?: return Result.retry()
            repository.install(release, file)
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "hush.updates"

        /** Enqueue or cancel to match the setting. Safe to call on every app start. */
        fun sync(context: Context) {
            val wm = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // The worker itself exits at once when autoCheck is off, so keeping it enqueued costs nothing.
            wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
