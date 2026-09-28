package com.nzrbits.hush.feature.wellbeing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.ShortVideoPlatform
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.actions.AccessibilityBridge
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import com.nzrbits.hush.feature.wellbeing.data.UsageLimitRepository
import com.nzrbits.hush.feature.wellbeing.domain.ShortVideoDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The only always-on component, and only when the user enables it in system settings.
 *
 * Responsibilities, each gated by a user setting:
 *  1. App blocking: when a blocked package comes to the foreground, go home and report the
 *     event so the launcher can show why.
 *  2. Usage limits: when a limited package comes to the foreground, run the limit check.
 *  3. Short video blocking: inspect the window of the four supported apps for Shorts/Reels
 *     markers and press back when found.
 *  4. Global actions (lock, notifications) through [AccessibilityBridge].
 *
 * Android 34+ restricts non-accessibility-tool apps from receiving events of all packages
 * unless the service config lists them; the config lists the four short video packages, and
 * `TYPE_WINDOW_STATE_CHANGED` for other packages is requested at runtime below.
 */
@AndroidEntryPoint
class HushAccessibilityService : AccessibilityService() {

    @Inject lateinit var bridge: AccessibilityBridge
    @Inject lateinit var blocking: BlockingRepository
    @Inject lateinit var limits: UsageLimitRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var clock: HushClock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastForeground: String? = null
    private var lastShortVideoActionAt = 0L
    private var lastBlockActionAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
        // Widen the package filter at runtime so blocking works for every app, not just the four
        // short video packages from the XML config.
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 200
            packageNames = null
        }
    }

    override fun onDestroy() {
        bridge.detach(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName == this.packageName) { lastForeground = packageName; return }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (isSystemUiPackage(packageName)) return
                val changed = packageName != lastForeground
                lastForeground = packageName
                if (changed) onForeground(packageName)
                maybeCheckShortVideo(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                maybeCheckShortVideo(packageName)
            }
        }
    }

    private fun isSystemUiPackage(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg == "android" || pkg.startsWith("com.android.inputmethod") || pkg.endsWith(".inputmethod.latin")

    private fun onForeground(packageName: String) {
        scope.launch {
            val reason = blocking.reasonFor(packageName)
            if (reason != null) {
                val now = clock.now().toEpochMilli()
                if (now - lastBlockActionAt > 1_500) {
                    lastBlockActionAt = now
                    blocking.reportBlocked(packageName, reason)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
                return@launch
            }
            limits.check(packageName)
        }
    }

    private fun maybeCheckShortVideo(packageName: String) {
        val platform = ShortVideoDetector.platformFor(packageName) ?: return
        val now = clock.now().toEpochMilli()
        if (now - lastShortVideoActionAt < 1_200) return
        val root = rootInActiveWindow ?: return
        scope.launch {
            val wellbeing = settings.settings.first().wellbeing
            if (!wellbeing.shortVideoBlockingEnabled || platform !in wellbeing.shortVideoPlatforms) return@launch
            val snapshot = collect(root)
            if (ShortVideoDetector.isShortVideoSurface(platform, snapshot.ids, snapshot.texts, snapshot.selectedTexts)) {
                lastShortVideoActionAt = clock.now().toEpochMilli()
                performGlobalAction(GLOBAL_ACTION_BACK)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@HushAccessibilityService, "${HushConfig.APP_NAME}: ${platform.displayName} ist ausgeschaltet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private class Snapshot(val ids: MutableSet<String> = HashSet(), val texts: MutableSet<String> = HashSet(), val selectedTexts: MutableSet<String> = HashSet())

    /** Bounded depth-first walk. Collects ids and texts only, never stores the tree. */
    private fun collect(root: AccessibilityNodeInfo): Snapshot {
        val snapshot = Snapshot()
        var visited = 0
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 24 || visited > 600) return
            visited++
            node.viewIdResourceName?.let { snapshot.ids += it }
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrEmpty()) { snapshot.texts += text; if (node.isSelected) snapshot.selectedTexts += text }
            if (!desc.isNullOrEmpty()) { snapshot.texts += desc; if (node.isSelected) snapshot.selectedTexts += desc }
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)
        return snapshot
    }

    companion object {
        val platforms: List<ShortVideoPlatform> = ShortVideoPlatform.entries
    }
}
