package com.nzrbits.hush.core.common

/** Filled by the app module at start so feature modules need no BuildConfig. */
object BuildInfo {
    var versionName: String = "dev"
    var versionCode: Int = 0

    /** "0.1.3-debug" -> "0.1.3"; "v0.1.4" -> "0.1.4". */
    fun normalize(version: String): String = version.trim().removePrefix("v").substringBefore('-')

    /** Compares dotted numeric versions; unknown parts count as 0. Positive when [a] is newer than [b]. */
    fun compare(a: String, b: String): Int {
        val pa = normalize(a).split('.').map { it.toIntOrNull() ?: 0 }
        val pb = normalize(b).split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = (pa.getOrNull(i) ?: 0) - (pb.getOrNull(i) ?: 0)
            if (d != 0) return d
        }
        return 0
    }
}
