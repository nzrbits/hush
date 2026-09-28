package com.nzrbits.hush.core.system.actions

import android.accessibilityservice.AccessibilityService
import android.util.Log
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a weak reference to the running accessibility service so UI code can ask for global
 * actions (lock screen, open notification shade, go home) without knowing the service class.
 * The service registers itself in onServiceConnected and clears itself in onDestroy.
 */
@Singleton
class AccessibilityBridge @Inject constructor() {
    private var serviceRef: WeakReference<AccessibilityService>? = null

    fun attach(service: AccessibilityService) {
        serviceRef = WeakReference(service)
        Log.i(TAG, "accessibility service attached")
    }

    fun detach(service: AccessibilityService) {
        if (serviceRef?.get() === service) serviceRef = null
        Log.i(TAG, "accessibility service detached")
    }

    val isConnected: Boolean get() = serviceRef?.get() != null

    fun lockScreen(): Boolean = perform("lock") { s ->
        if (android.os.Build.VERSION.SDK_INT < 28) false else s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    fun openNotifications(): Boolean = perform("notifications") { it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) }

    fun goHome(): Boolean = perform("home") { it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME) }

    fun goBack(): Boolean = perform("back") { it.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK) }

    private fun perform(name: String, action: (AccessibilityService) -> Boolean): Boolean {
        val service = serviceRef?.get()
        if (service == null) {
            Log.i(TAG, "global action '$name' requested but service not connected")
            return false
        }
        val ok = action(service)
        Log.i(TAG, "global action '$name' -> $ok")
        return ok
    }

    private companion object { const val TAG = "HushBridge" }
}
