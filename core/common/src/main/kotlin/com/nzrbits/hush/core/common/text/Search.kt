package com.nzrbits.hush.core.common.text

import java.text.Normalizer
import java.util.Locale

/**
 * Search helpers for the app drawer. Labels are normalised once (lower case, accents removed)
 * so filtering a few hundred apps on every keystroke stays cheap.
 */
object Search {
    private val diacritics = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalize(text: String): String =
        diacritics.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
            .lowercase(Locale.ROOT)
            .trim()

    /**
     * Ranks a normalised label against a normalised query. Lower is better, null means no match.
     * Prefix matches rank before word-start matches, which rank before substring matches.
     */
    fun rank(normalizedLabel: String, normalizedQuery: String): Int? {
        if (normalizedQuery.isEmpty()) return 0
        if (normalizedLabel.startsWith(normalizedQuery)) return 0
        val wordStart = normalizedLabel.split(' ', '-', '_', '.').any { it.startsWith(normalizedQuery) }
        if (wordStart) return 1
        if (normalizedLabel.contains(normalizedQuery)) return 2
        return null
    }

    /** First letter for the alphabetical index. Digits and symbols map to '#'. */
    fun indexLetter(label: String): Char {
        val c = normalize(label).firstOrNull() ?: return '#'
        return if (c in 'a'..'z') c.uppercaseChar() else '#'
    }
}
