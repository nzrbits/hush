package com.nzrbits.hush.core.system.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.view.accessibility.AccessibilityManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.nzrbits.hush.core.system.usage.UsageStatsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class HushPermission(val title: String) {
    DEFAULT_LAUNCHER("Standard-Launcher"),
    USAGE_ACCESS("Nutzungszugriff"),
    NOTIFICATION_ACCESS("Benachrichtigungszugriff"),
    ACCESSIBILITY("Bedienungshilfe"),
    POST_NOTIFICATIONS("Eigene Benachrichtigungen"),
}

data class PermissionState(val permission: HushPermission, val granted: Boolean)

/**
 * Central read-only view of every special permission Hush uses. Nothing here requests a
 * permission silently. Each feature opens the matching system screen with a clear explanation.
 */
@Singleton
class PermissionsChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usage: UsageStatsSource,
) {
    /** Fully-qualified service names are set by the app module so this core module stays generic. */
    var accessibilityServiceClass: String = ""
    var notificationListenerClass: String = ""

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun hasUsageAccess(): Boolean = usage.hasUsageAccess()

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val component = ComponentName(context.packageName, notificationListenerClass).flattenToString()
        return enabled.split(':').any { it == component || it == "${context.packageName}/${notificationListenerClass.removePrefix(context.packageName)}" }
    }

    fun isAccessibilityEnabled(): Boolean {
        if (accessibilityServiceClass.isEmpty()) return false
        // Primary: ask the AccessibilityManager, which reflects the live bound state.
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val live = manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { info ->
                val id = info.id ?: ""
                id.startsWith(context.packageName + "/") && id.endsWith(accessibilityServiceClass.substringAfterLast('.'))
            } ?: false
        if (live) return true
        // Fallback: the settings string, which some OEMs update before binding.
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val flat = ComponentName(context.packageName, accessibilityServiceClass).flattenToString()
        val short = ComponentName(context.packageName, accessibilityServiceClass).flattenToShortString()
        return enabled.split(':').any { it.equals(flat, ignoreCase = true) || it.equals(short, ignoreCase = true) }
    }

    fun canPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        }

    fun snapshot(): List<PermissionState> = listOf(
        PermissionState(HushPermission.DEFAULT_LAUNCHER, isDefaultLauncher()),
        PermissionState(HushPermission.USAGE_ACCESS, hasUsageAccess()),
        PermissionState(HushPermission.NOTIFICATION_ACCESS, hasNotificationAccess()),
        PermissionState(HushPermission.ACCESSIBILITY, isAccessibilityEnabled()),
        PermissionState(HushPermission.POST_NOTIFICATIONS, canPostNotifications()),
    )

    /** Intent that opens the right system screen for the permission. */
    fun settingsIntent(permission: HushPermission): Intent = when (permission) {
        HushPermission.DEFAULT_LAUNCHER -> defaultLauncherIntent()
        HushPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        HushPermission.NOTIFICATION_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        HushPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        HushPermission.POST_NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun defaultLauncherIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
