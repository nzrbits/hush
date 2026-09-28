package com.nzrbits.hush.core.system.usage

import com.nzrbits.hush.core.common.model.AppUsage

/** Minimal event model so the aggregation logic can be unit tested without Android. */
data class ForegroundEvent(val packageName: String, val timestamp: Long, val kind: Kind) {
    enum class Kind { RESUMED, PAUSED }
}

/**
 * Turns a stream of resumed/paused events into per-package foreground time inside a window.
 * Handles apps that were already in the foreground at window start and still are at window end.
 */
object UsageAggregator {
    fun aggregate(events: List<ForegroundEvent>, windowStart: Long, windowEnd: Long): List<AppUsage> {
        val openSince = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        val launches = HashMap<String, Int>()
        for (event in events.sortedBy { it.timestamp }) {
            when (event.kind) {
                ForegroundEvent.Kind.RESUMED -> {
                    if (!openSince.containsKey(event.packageName)) {
                        openSince[event.packageName] = event.timestamp.coerceAtLeast(windowStart)
                        launches[event.packageName] = (launches[event.packageName] ?: 0) + 1
                    }
                }
                ForegroundEvent.Kind.PAUSED -> {
                    val start = openSince.remove(event.packageName) ?: continue
                    val end = event.timestamp.coerceAtMost(windowEnd)
                    if (end > start) total[event.packageName] = (total[event.packageName] ?: 0L) + (end - start)
                }
            }
        }
        // Still open at the end of the window.
        for ((pkg, start) in openSince) {
            if (windowEnd > start) total[pkg] = (total[pkg] ?: 0L) + (windowEnd - start)
        }
        return total.map { (pkg, millis) -> AppUsage(pkg, millis, launches[pkg] ?: 0) }
            .sortedByDescending { it.foregroundMillis }
    }
}
