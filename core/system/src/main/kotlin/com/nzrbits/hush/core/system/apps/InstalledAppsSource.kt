package com.nzrbits.hush.core.system.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.model.AppKey
import com.nzrbits.hush.core.common.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads launchable apps from [LauncherApps] for every user profile (personal and work) and
 * emits a fresh list whenever a package is added, removed or changed. The callback only
 * signals; the actual (slow) reload runs on the IO dispatcher.
 */
@Singleton
class InstalledAppsSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val selfPackage = context.packageName

    /** LauncherApps delivers callbacks on a Handler; registration itself needs a Looper thread. */
    private val callbackHandler = Handler(Looper.getMainLooper())

    private val changes: Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageAdded(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackageChanged(packageName: String?, user: UserHandle?) { trySend(Unit) }
            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) { trySend(Unit) }
            override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) { trySend(Unit) }
            override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) { trySend(Unit) }
        }
        launcherApps.registerCallback(callback, callbackHandler)
        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    @OptIn(ExperimentalCoroutinesApi::class)
    val apps: Flow<List<InstalledApp>> = changes
        .onStart { emit(Unit) }
        .mapLatest { load() }
        .flowOn(dispatchers.io)

    fun load(): List<InstalledApp> {
        val result = ArrayList<InstalledApp>()
        val personalSerial = userManager.getSerialNumberForUser(Process.myUserHandle())
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
            for (info in activities) {
                val pkg = info.applicationInfo.packageName
                if (pkg == selfPackage) continue
                result += InstalledApp(
                    key = AppKey(pkg, serial),
                    label = info.label?.toString() ?: pkg,
                    activityClassName = info.componentName.className,
                    isWorkProfile = serial != personalSerial,
                    isSystemApp = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }
        return result.distinctBy { it.key.id + it.activityClassName }
    }

    private fun userHandle(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    suspend fun launch(key: AppKey, activityClassName: String): Boolean = withContext(dispatchers.io) {
        val user = userHandle(key.userSerial) ?: return@withContext false
        runCatching {
            val component = android.content.ComponentName(key.packageName, activityClassName)
            launcherApps.startMainActivity(component, user, null, null)
            true
        }.getOrElse {
            // Fall back to a plain launch intent for the personal profile.
            val intent = context.packageManager.getLaunchIntentForPackage(key.packageName) ?: return@getOrElse false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.isSuccess
        }
    }

    fun openAppInfo(key: AppKey, activityClassName: String) {
        val user = userHandle(key.userSerial)
        val component = android.content.ComponentName(key.packageName, activityClassName)
        val ok = runCatching {
            if (user != null) launcherApps.startAppDetailsActivity(component, user, null, null)
            true
        }.getOrDefault(false)
        if (!ok) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${key.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    fun requestUninstall(key: AppKey) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${key.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Human label; for an app that is not installed, the last package segment plus a note instead of the raw package. */
    fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrElse {
        // com.instagram.android -> "Instagram", com.google.android.youtube -> "Youtube"
        val generic = setOf("android", "app", "apps", "mobile", "client", "main")
        val segment = packageName.split('.').lastOrNull { it.lowercase() !in generic } ?: packageName
        "${segment.replaceFirstChar { it.uppercase() }} (nicht installiert)"
    }
}
