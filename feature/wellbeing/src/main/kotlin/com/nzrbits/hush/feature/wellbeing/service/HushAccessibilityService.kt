package com.nzrbits.hush.feature.wellbeing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.ShortVideoPlatform
import com.nzrbits.hush.core.common.model.WellbeingSettings
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.datastore.SettingsRepository
import com.nzrbits.hush.core.system.actions.AccessibilityBridge
import com.nzrbits.hush.core.system.apps.ProtectedPackages
import com.nzrbits.hush.feature.wellbeing.data.BlockingRepository
import com.nzrbits.hush.feature.wellbeing.data.UsageLimitRepository
import com.nzrbits.hush.feature.wellbeing.domain.ShortVideoDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/**
 * The only always-on component, and only when the user enables it in system settings.
 *
 * Responsibilities, each gated by a user setting:
 *  1. App blocking: when a blocked package is in the foreground, go home and report the
 *     event so the launcher can show why. Re-evaluated on every foreground change and every
 *     time the block state changes (minute ticker, new block, schedule start).
 *  2. Usage limits: when a limited package comes to the foreground, run the limit check.
 *  3. Short video blocking: inspect the window of the four supported apps for Shorts/Reels
 *     markers and press back when found. Throttled to one walk per 500 ms, never concurrent.
 *  4. Global actions (lock, notifications) through [AccessibilityBridge].
 *
 * Protected packages (dialer, settings, keyboards, ...) are never acted on, see [ProtectedPackages].
 */
@AndroidEntryPoint
class HushAccessibilityService : AccessibilityService() {

    @Inject lateinit var bridge: AccessibilityBridge
    @Inject lateinit var blocking: BlockingRepository
    @Inject lateinit var limits: UsageLimitRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var protectedPackages: ProtectedPackages
    @Inject lateinit var clock: HushClock

    private val handler = CoroutineExceptionHandler { _, e -> Log.w(TAG, "background error", e) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    private lateinit var wellbeing: StateFlow<WellbeingSettings>

    @Volatile private var lastForeground: String? = null
    @Volatile private var lastShortVideoActionAt = 0L
    @Volatile private var lastWalkAt = 0L
    @Volatile private var lastReportAt = 0L
    @Volatile private var imePackages: Set<String> = emptySet()
    private val walkMutex = Mutex()

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
        imePackages = protectedPackages.imePackages()
        wellbeing = settings.settings.map { it.wellbeing }.stateIn(scope, SharingStarted.Eagerly, WellbeingSettings())
        // Widen the package filter at runtime so blocking works for every app, not just the four
        // short video packages from the XML config.
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 200
            packageNames = null
        }
        // Re-check the current foreground app whenever block state changes, so a schedule that
        // starts while the app is open still takes effect within a minute.
        scope.launch {
            blocking.status.collect { lastForeground?.let { pkg -> enforce(pkg, checkLimit = false) } }
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
        if (isSystemUiPackage(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val changed = packageName != lastForeground
                lastForeground = packageName
                if (changed) scope.launch { enforce(packageName, checkLimit = true) }
                maybeCheckShortVideo(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> maybeCheckShortVideo(packageName)
        }
    }

    private fun isSystemUiPackage(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg == "android" || pkg in imePackages ||
            pkg.startsWith("com.android.inputmethod") || pkg.endsWith(".inputmethod.latin")

    /** Blocked -> always go home; the report to the launcher is debounced, the action is not. */
    private suspend fun enforce(packageName: String, checkLimit: Boolean) {
        if (protectedPackages.isProtected(packageName)) return
        val reason = blocking.reasonFor(packageName)
        if (reason != null) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            val now = clock.now().toEpochMilli()
            if (now - lastReportAt > 1_500) {
                lastReportAt = now
                blocking.reportBlocked(packageName, reason)
            }
            return
        }
        if (checkLimit) limits.check(packageName)
    }

    private fun maybeCheckShortVideo(packageName: String) {
        val platform = ShortVideoDetector.platformFor(packageName) ?: return
        val w = wellbeing.value
        if (!w.shortVideoBlockingEnabled || platform !in w.shortVideoPlatforms) return
        val now = clock.now().toEpochMilli()
        if (now - lastShortVideoActionAt < 1_200 || now - lastWalkAt < 500) return
        if (!walkMutex.tryLock()) return
        lastWalkAt = now
        val root = rootInActiveWindow
        if (root == null) { walkMutex.unlock(); return }
        scope.launch {
            try {
                val snapshot = collect(root)
                if (ShortVideoDetector.isShortVideoSurface(platform, snapshot.ids, snapshot.texts, snapshot.selectedTexts)) {
                    lastShortVideoActionAt = clock.now().toEpochMilli()
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@HushAccessibilityService, "${HushConfig.APP_NAME}: ${platform.displayName} ist ausgeschaltet.", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                walkMutex.unlock()
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
            runCatching {
                node.viewIdResourceName?.let { snapshot.ids += it }
                val text = node.text?.toString()?.trim()
                val desc = node.contentDescription?.toString()?.trim()
                if (!text.isNullOrEmpty()) { snapshot.texts += text; if (node.isSelected) snapshot.selectedTexts += text }
                if (!desc.isNullOrEmpty()) { snapshot.texts += desc; if (node.isSelected) snapshot.selectedTexts += desc }
                for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
            }
        }
        walk(root, 0)
        return snapshot
    }

    companion object {
        private const val TAG = "HushA11y"
        val platforms: List<ShortVideoPlatform> = ShortVideoPlatform.entries
    }
}
