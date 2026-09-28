package com.nzrbits.hush.core.system.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Packages Hush must never block or filter: the launcher itself, system UI, settings, the
 * dialer and in-call UI, SMS and emergency apps, the alarm clock, the package installer and
 * permission controller, and every installed keyboard. Blocking any of these would lock the
 * user out of calls, alarms or the way to switch the accessibility service off again.
 */
@Singleton
class ProtectedPackages @Inject constructor(@ApplicationContext private val context: Context) {

    private val fixed = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.emergency",
    )

    /** Recomputed on each call because defaults can change; cheap (a few system queries). */
    fun all(): Set<String> {
        val result = HashSet(fixed)
        result += context.packageName
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()?.let { result += it }
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()?.let { result += it }
        resolve(Intent(AlarmClock.ACTION_SHOW_ALARMS))?.let { result += it }
        resolve(Intent(Intent.ACTION_DIAL))?.let { result += it }
        result += imePackages()
        return result
    }

    fun isProtected(packageName: String): Boolean = packageName in all()

    fun imePackages(): Set<String> = runCatching {
        context.getSystemService(InputMethodManager::class.java)?.inputMethodList?.map { it.packageName }?.toSet()
    }.getOrNull() ?: emptySet()

    private fun resolve(intent: Intent): String? = runCatching {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }.getOrNull()?.takeIf { it != "android" }
}
