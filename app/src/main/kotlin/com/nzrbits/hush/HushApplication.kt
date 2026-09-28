package com.nzrbits.hush

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nzrbits.hush.core.system.notify.HushNotifications
import com.nzrbits.hush.core.system.permissions.PermissionsChecker
import com.nzrbits.hush.feature.notifications.service.HushNotificationListener
import com.nzrbits.hush.feature.settings.BuildInfo
import com.nzrbits.hush.feature.wellbeing.service.HushAccessibilityService
import com.nzrbits.hush.feature.wellbeing.service.UsageLimitWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HushApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var permissions: PermissionsChecker
    @Inject lateinit var notifications: HushNotifications

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        BuildInfo.versionName = BuildConfig.VERSION_NAME
        permissions.accessibilityServiceClass = HushAccessibilityService::class.java.name
        permissions.notificationListenerClass = HushNotificationListener::class.java.name
        notifications.ensureChannels()
        UsageLimitWorker.schedule(this)
    }
}
