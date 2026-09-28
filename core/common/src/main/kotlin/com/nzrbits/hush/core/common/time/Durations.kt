package com.nzrbits.hush.core.common.time

/** Human readable German durations, used in the block UI and in screen time. */
object Durations {
    fun formatMinutes(totalMinutes: Long): String {
        if (totalMinutes < 60) return "$totalMinutes Min."
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        val parts = mutableListOf<String>()
        if (days > 0) parts += if (days == 1L) "1 Tag" else "$days Tage"
        if (hours > 0) parts += if (hours == 1L) "1 Std." else "$hours Std."
        if (minutes > 0 && days == 0L) parts += "$minutes Min."
        return parts.joinToString(" ")
    }

    fun formatMillisShort(millis: Long): String {
        val minutes = millis / 60_000
        if (minutes < 1) return "<1 Min."
        if (minutes < 60) return "$minutes Min."
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "$h Std." else "$h Std. $m Min."
    }
}
