package com.nzrbits.hush.core.system.usage

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.system.usage.ForegroundEvent.Kind
import org.junit.Test

class UsageAggregatorTest {
    private fun ev(pkg: String, t: Long, kind: Kind) = ForegroundEvent(pkg, t, kind)

    @Test
    fun sumsResumedPausedPairs() {
        val events = listOf(
            ev("a", 1_000, Kind.RESUMED), ev("a", 4_000, Kind.PAUSED),
            ev("b", 4_000, Kind.RESUMED), ev("b", 5_000, Kind.PAUSED),
            ev("a", 6_000, Kind.RESUMED), ev("a", 8_000, Kind.PAUSED),
        )
        val result = UsageAggregator.aggregate(events, 0, 10_000).associateBy { it.packageName }
        assertThat(result.getValue("a").foregroundMillis).isEqualTo(5_000)
        assertThat(result.getValue("a").launchCount).isEqualTo(2)
        assertThat(result.getValue("b").foregroundMillis).isEqualTo(1_000)
    }

    @Test
    fun appOpenAtWindowStartIsClippedToWindow() {
        val events = listOf(ev("a", -5_000, Kind.RESUMED), ev("a", 2_000, Kind.PAUSED))
        val result = UsageAggregator.aggregate(events, 0, 10_000)
        assertThat(result.single().foregroundMillis).isEqualTo(2_000)
    }

    @Test
    fun appStillOpenAtWindowEndCountsUntilEnd() {
        val events = listOf(ev("a", 7_000, Kind.RESUMED))
        val result = UsageAggregator.aggregate(events, 0, 10_000)
        assertThat(result.single().foregroundMillis).isEqualTo(3_000)
    }

    @Test
    fun pausedWithoutResumedIsIgnored() {
        val events = listOf(ev("a", 3_000, Kind.PAUSED))
        assertThat(UsageAggregator.aggregate(events, 0, 10_000)).isEmpty()
    }

    @Test
    fun duplicateResumedDoesNotDoubleCount() {
        val events = listOf(ev("a", 1_000, Kind.RESUMED), ev("a", 2_000, Kind.RESUMED), ev("a", 4_000, Kind.PAUSED))
        val result = UsageAggregator.aggregate(events, 0, 10_000).single()
        assertThat(result.foregroundMillis).isEqualTo(3_000)
        assertThat(result.launchCount).isEqualTo(1)
    }

    @Test
    fun sortedByTimeDescending() {
        val events = listOf(
            ev("small", 0, Kind.RESUMED), ev("small", 100, Kind.PAUSED),
            ev("big", 200, Kind.RESUMED), ev("big", 5_000, Kind.PAUSED),
        )
        assertThat(UsageAggregator.aggregate(events, 0, 10_000).map { it.packageName }).containsExactly("big", "small").inOrder()
    }
}
